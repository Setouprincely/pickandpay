package com.doch.common.geocoding.impl;

import com.doch.Logistic.dto.RouteStopDTO;
import com.doch.Logistic.model.Delivery;
import com.doch.Logistic.model.DeliveryStatus;
import com.doch.Logistic.repository.DeliveryRepository;
import com.doch.User.model.DriverProfile;
import com.doch.User.model.User;
import com.doch.User.repository.DriverProfileRepository;
import com.doch.common.geocoding.NavigationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * Builds the driver's multi-stop trip from all active jobs.
 * Uses a nearest-neighbour heuristic starting from the driver's last GPS position,
 * with the precedence constraint that a package's pickup always comes before its drop-off.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GoogleNavigationServiceImpl implements NavigationService {

    private final DeliveryRepository deliveryRepository;
    private final DriverProfileRepository driverProfileRepository;

    @Override
    @Transactional(readOnly = true)
    public List<RouteStopDTO> getOptimizedRoute(User driver) {
        List<Delivery> active = deliveryRepository.findByDriverAndStatusNotIn(driver,
                List.of(DeliveryStatus.DELIVERED, DeliveryStatus.CANCELLED, DeliveryStatus.RETURNED));

        List<RouteStopDTO> pending = new ArrayList<>();
        Map<String, RouteStopDTO> dropOffs = new HashMap<>();
        for (Delivery d : active) {
            RouteStopDTO drop = stop(d, "DROP_OFF");
            if (d.getStatus() == DeliveryStatus.DRIVER_ASSIGNED) {
                pending.add(stop(d, "PICKUP"));
                dropOffs.put(d.getId(), drop); // released once the pickup is sequenced
            } else if (d.getStatus() == DeliveryStatus.PICKED_UP || d.getStatus() == DeliveryStatus.IN_TRANSIT
                    || d.getStatus() == DeliveryStatus.FAILED) {
                pending.add(drop);
            }
        }

        double[] position = driverPosition(driver);
        List<RouteStopDTO> ordered = new ArrayList<>();
        while (!pending.isEmpty()) {
            RouteStopDTO next = pending.get(0);
            if (position != null) {
                double best = Double.MAX_VALUE;
                for (RouteStopDTO candidate : pending) {
                    double km = distanceKm(position, coords(candidate));
                    if (km < best) {
                        best = km;
                        next = candidate;
                    }
                }
            }
            pending.remove(next);
            double[] target = coords(next);
            if (position != null && target != null) {
                next.setLegDistanceKm(Math.round(distanceKm(position, target) * 10.0) / 10.0);
            }
            next.setSequence(ordered.size() + 1);
            ordered.add(next);
            if (target != null) position = target;
            if ("PICKUP".equals(next.getStopType()) && dropOffs.containsKey(next.getDeliveryId())) {
                pending.add(dropOffs.remove(next.getDeliveryId()));
            }
        }
        return ordered;
    }

    private double[] driverPosition(User driver) {
        Optional<DriverProfile> profile = driverProfileRepository.findByUser(driver);
        if (profile.isEmpty() || profile.get().getLastLatitude() == null || profile.get().getLastLongitude() == null) {
            return null;
        }
        return parse(profile.get().getLastLatitude(), profile.get().getLastLongitude());
    }

    private RouteStopDTO stop(Delivery d, String type) {
        RouteStopDTO.RouteStopDTOBuilder b = RouteStopDTO.builder()
                .deliveryId(d.getId())
                .trackingNumber(d.getTrackingNumber())
                .deliveryStatus(d.getStatus().name())
                .packageWeightKg(d.getPackageWeightKg())
                .stopType(type);
        if (d.getMarketDeliveryRequest() != null) {
            b.orderReference(d.getMarketDeliveryRequest().getOrder().getReference());
        }
        if ("PICKUP".equals(type)) {
            b.address(d.getPickupAddress());
            if (d.getDeliveryRequest() != null) {
                setCoords(b, d.getDeliveryRequest().getPickupLocation());
                User sender = d.getDeliveryRequest().getSender();
                b.contactName(sender != null ? sender.getFirstName() + " " + sender.getLastName() : "Sender")
                        .contactPhone(sender != null ? sender.getTel() : null);
            } else if (d.getMarketDeliveryRequest() != null) {
                setCoords(b, d.getMarketDeliveryRequest().getPickupLocation());
                var seller = d.getMarketDeliveryRequest().getSeller();
                b.contactName(seller != null ? seller.getName() : "Seller")
                        .contactPhone(seller != null ? seller.getContactNumber() : null);
            }
        } else {
            b.address(d.getDropoffAddress());
            if (d.getDeliveryRequest() != null) {
                setCoords(b, d.getDeliveryRequest().getReceiverLocation());
                User sender = d.getDeliveryRequest().getSender();
                b.contactName("Recipient").contactPhone(sender != null ? sender.getTel() : null);
            } else if (d.getMarketDeliveryRequest() != null) {
                setCoords(b, d.getMarketDeliveryRequest().getReceiverLocation());
                var order = d.getMarketDeliveryRequest().getOrder();
                b.contactName(order.getCustomer().getFirstName() + " " + order.getCustomer().getLastName())
                        .contactPhone(order.getContactPhone() != null ? order.getContactPhone() : order.getCustomer().getTel());
            }
        }
        return b.build();
    }

    private static void setCoords(RouteStopDTO.RouteStopDTOBuilder b, com.doch.Order.model.Location location) {
        if (location != null) {
            b.latitude(location.getLatitude()).longitude(location.getLongitude());
        }
    }

    private static double[] coords(RouteStopDTO stop) {
        return parse(stop.getLatitude(), stop.getLongitude());
    }

    private static double[] parse(String lat, String lng) {
        try {
            if (lat == null || lng == null) return null;
            return new double[]{Double.parseDouble(lat), Double.parseDouble(lng)};
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** Haversine distance in km. */
    private static double distanceKm(double[] a, double[] b) {
        if (a == null || b == null) return Double.MAX_VALUE / 4;
        double dLat = Math.toRadians(b[0] - a[0]);
        double dLng = Math.toRadians(b[1] - a[1]);
        double h = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(a[0])) * Math.cos(Math.toRadians(b[0])) * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        return 6371 * 2 * Math.atan2(Math.sqrt(h), Math.sqrt(1 - h));
    }
}

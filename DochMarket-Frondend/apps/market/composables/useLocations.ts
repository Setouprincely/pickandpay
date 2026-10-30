// Cameroon location hierarchy for the Market checkout address picker:
//   Region → City → Subdivision (arrondissement) → free-text area/landmark.
//
// Same cascade idea as the Xender app: it mirrors how people here describe where
// they are ("Littoral → Douala → Bonabéri → près du carrefour Total") and keeps
// each list short instead of one giant scroll. Douala, Yaoundé, Buea, and Bamenda
// are populated; other regions can be filled in the same way as demand grows.
//
// Coordinates are at the subdivision (city-district) level — accurate enough to
// place someone in the right part of town, not to a specific street. Worth a
// quick check by someone with local knowledge of Buea/Bamenda before this goes
// live for anything price-sensitive, same as any new region added here.

export interface Subdivision {
  id: string
  name: string
  lat: number
  lng: number
  /** Known quartiers — used to autocomplete the area field. Free text is also allowed. */
  quartiers: string[]
}

export interface City {
  id: string
  name: string
  comingSoon?: boolean
  subdivisions: Subdivision[]
}

export interface Region {
  id: string
  name: string
  cities: City[]
}

/** The structured value the LocationPicker produces. */
export interface PickedLocation {
  regionId: string
  regionName: string
  cityId: string
  cityName: string
  subdivisionId: string
  subdivisionName: string
  /** Free-text exact spot / nearest landmark (optional). */
  landmark: string
  lat: number
  lng: number
  /** Friendly one-line label for compact displays. */
  label: string
}

export const CAMEROON_REGIONS: Region[] = [
  {
    id: 'littoral',
    name: 'Littoral',
    cities: [
      {
        id: 'douala',
        name: 'Douala',
        subdivisions: [
          { id: 'douala-1', name: 'Douala I (Akwa)', lat: 4.0500, lng: 9.7000, quartiers: ['Akwa', 'Bonanjo', 'Bonapriso', 'Bali', 'Deïdo', 'Bonadibong', 'Joss'] },
          { id: 'douala-2', name: 'Douala II (New Bell)', lat: 4.0380, lng: 9.7250, quartiers: ['New Bell', 'Congo', 'Nkololoun', 'Ngangue', 'Douala-Bell', 'Lagos'] },
          { id: 'douala-3', name: 'Douala III (Logbaba)', lat: 4.0050, lng: 9.7550, quartiers: ['Logbaba', 'Ndogbong', 'Ndogpassi', 'PK8', 'PK10', 'PK12', 'Yassa', 'Cité des Palmiers', 'Village'] },
          { id: 'douala-4', name: 'Douala IV (Bonabéri)', lat: 4.0780, lng: 9.6800, quartiers: ['Bonabéri', 'Bonassama', 'Sodiko', 'Mabanda', 'Bojongo', 'Grand Hangar'] },
          { id: 'douala-5', name: 'Douala V (Bonamoussadi)', lat: 4.0900, lng: 9.7480, quartiers: ['Bonamoussadi', 'Makepe', 'Kotto', 'Logpom', 'Bépanda', 'Logbessou', 'Denver', 'Carrefour Maçon'] },
        ],
      },
    ],
  },
  {
    id: 'centre',
    name: 'Centre',
    cities: [
      {
        id: 'yaounde',
        name: 'Yaoundé',
        subdivisions: [
          { id: 'yaounde-1', name: 'Yaoundé I (Nlongkak)', lat: 3.8950, lng: 11.5220, quartiers: ['Bastos', 'Nlongkak', 'Briqueterie', 'Quartier du Lac', 'Hippodrome', 'Carrière', 'Etoa-Meki'] },
          { id: 'yaounde-2', name: 'Yaoundé II (Tsinga)', lat: 3.8830, lng: 11.5050, quartiers: ['Tsinga', 'Mokolo', 'Madagascar', 'Ekoudou', 'Messa', 'Cité Verte'] },
          { id: 'yaounde-3', name: 'Yaoundé III (Efoulan)', lat: 3.8350, lng: 11.5000, quartiers: ['Efoulan', 'Mvog-Mbi', 'Nsam', 'Obili', 'Mvog-Betsi', 'Ngoa-Ekellé'] },
          { id: 'yaounde-4', name: 'Yaoundé IV (Kondengui)', lat: 3.8650, lng: 11.5450, quartiers: ['Kondengui', 'Nkolndongo', 'Mvog-Ada', 'Ekounou', 'Mimboman', 'Awae', 'Nkomo'] },
          { id: 'yaounde-5', name: 'Yaoundé V (Essos)', lat: 3.8950, lng: 11.5400, quartiers: ['Essos', 'Nkolmesseng', 'Messamendongo', 'Emana', 'Awae Escalier'] },
          { id: 'yaounde-6', name: 'Yaoundé VI (Biyem-Assi)', lat: 3.8400, lng: 11.4750, quartiers: ['Biyem-Assi', 'Mendong', 'Etoug-Ebe', 'Melen', 'Simbock'] },
          { id: 'yaounde-7', name: 'Yaoundé VII (Nkolbisson)', lat: 3.8800, lng: 11.4350, quartiers: ['Nkolbisson', 'Oyom-Abang', 'Nkolso'] },
        ],
      },
    ],
  },
  {
    id: 'sud-ouest',
    name: 'Sud-Ouest',
    cities: [
      {
        id: 'buea',
        name: 'Buea',
        subdivisions: [
          { id: 'buea-station', name: 'Buea Station (Buea Town)', lat: 4.1520, lng: 9.2350, quartiers: ['Buea Town', 'Clerks Quarter', 'Federal Quarter', 'GRA', 'Bonduma', 'Bokwango'] },
          { id: 'molyko', name: 'Molyko', lat: 4.1450, lng: 9.2700, quartiers: ['Molyko', 'Mile 17', 'Sandpit', 'Wonganga'] },
          { id: 'soppo', name: 'Soppo', lat: 4.1650, lng: 9.2500, quartiers: ['Great Soppo', 'Small Soppo', 'Bokwai', 'Likoko Membea'] },
          { id: 'muea', name: 'Muea', lat: 4.1150, lng: 9.2450, quartiers: ['Muea', 'Bomaka', 'Bolifamba (Mile 16)', 'Dibanda (Mile 14)'] },
        ],
      },
    ],
  },
  {
    id: 'nord-ouest',
    name: 'Nord-Ouest',
    cities: [
      {
        id: 'bamenda',
        name: 'Bamenda',
        subdivisions: [
          { id: 'bamenda-1', name: 'Bamenda I (Up Station)', lat: 5.9631, lng: 10.1591, quartiers: ['Up Station', 'Old Town', 'City Chemist', 'Hospital Roundabout', 'Ntamulung', 'Cow Street'] },
          { id: 'bamenda-2', name: 'Bamenda II (Mankon)', lat: 5.9720, lng: 10.1280, quartiers: ['Mankon', 'Mulang', 'Ntarikon', 'Atuakom', 'Meta Quarter', 'Foncha Street'] },
          { id: 'bamenda-3', name: 'Bamenda III (Nkwen)', lat: 5.9850, lng: 10.1650, quartiers: ['Nkwen', 'Mile 3 Nkwen', 'Mile 4 Nkwen', 'Ndzah', 'Chomba', 'Mendankwe'] },
        ],
      },
    ],
  },
]

export function useLocations() {
  function regions() {
    return CAMEROON_REGIONS
  }

  function getRegion(regionId: string) {
    return CAMEROON_REGIONS.find(r => r.id === regionId) || null
  }

  function citiesFor(regionId: string): City[] {
    return getRegion(regionId)?.cities ?? []
  }

  function getCity(regionId: string, cityId: string): City | null {
    return citiesFor(regionId).find(c => c.id === cityId) || null
  }

  function subdivisionsFor(regionId: string, cityId: string): Subdivision[] {
    return getCity(regionId, cityId)?.subdivisions ?? []
  }

  function getSubdivision(regionId: string, cityId: string, subId: string): Subdivision | null {
    return subdivisionsFor(regionId, cityId).find(s => s.id === subId) || null
  }

  /** Build the structured value once region + city + subdivision are chosen. */
  function build(regionId: string, cityId: string, subId: string, landmark: string): PickedLocation | null {
    const region = getRegion(regionId)
    const city = getCity(regionId, cityId)
    const sub = getSubdivision(regionId, cityId, subId)
    if (!region || !city || !sub) return null
    const spot = landmark.trim()
    return {
      regionId, regionName: region.name,
      cityId, cityName: city.name,
      subdivisionId: subId, subdivisionName: sub.name,
      landmark: spot,
      lat: sub.lat, lng: sub.lng,
      label: spot ? `${sub.name} — ${spot}` : sub.name,
    }
  }

  return { regions, getRegion, citiesFor, getCity, subdivisionsFor, getSubdivision, build }
}

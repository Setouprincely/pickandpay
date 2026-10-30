package com.doch.Catalog.model;

/**
 * Marketplace publication lifecycle of a product listing.
 * Only PUBLISHED, active products of verified sellers are visible to customers.
 */
public enum ProductPublicationStatus {
    /** Submitted by the seller, waiting for an administrator decision. */
    PENDING_REVIEW,
    /** Approved (or auto-approved when moderation is disabled) and visible in the marketplace. */
    PUBLISHED,
    /** Refused by an administrator. The seller may edit and resubmit. */
    REJECTED
}

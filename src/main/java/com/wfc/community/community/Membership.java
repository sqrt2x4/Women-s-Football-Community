package com.wfc.community.community;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(
        name = "community_memberships",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_membership_community_name",
                columnNames = {"community_id", "display_name"}))
public class Membership {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "community_id", nullable = false)
    private Community community;

    @Column(name = "display_name", nullable = false, length = 40)
    private String displayName;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant joinedAt;

    protected Membership() {
    }

    Membership(Community community, String displayName) {
        this.community = community;
        this.displayName = displayName;
    }

    public UUID getId() {
        return id;
    }

    public Community getCommunity() {
        return community;
    }

    public String getDisplayName() {
        return displayName;
    }

    public Instant getJoinedAt() {
        return joinedAt;
    }
}

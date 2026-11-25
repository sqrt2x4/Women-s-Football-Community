package com.wfc.community.community;

import com.wfc.community.auth.UserAccount;
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
        name = "user_community_memberships",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_membership_community_user",
                columnNames = {"community_id", "user_id"}))
public class Membership {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "community_id", nullable = false)
    private Community community;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserAccount user;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant joinedAt;

    protected Membership() {
    }

    Membership(Community community, UserAccount user) {
        this.community = community;
        this.user = user;
    }

    public UUID getId() {
        return id;
    }

    public Community getCommunity() {
        return community;
    }

    public String getDisplayName() {
        return user.getDisplayName();
    }

    public UserAccount getUser() {
        return user;
    }

    public Instant getJoinedAt() {
        return joinedAt;
    }
}

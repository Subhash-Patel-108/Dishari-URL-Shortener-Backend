package com.dishari.in.domain.entity;

import com.dishari.in.domain.enums.WorkspaceMemberRole;
import com.dishari.in.domain.enums.WorkspaceMemberStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.SoftDelete;
import org.hibernate.annotations.SoftDeleteType;

import java.time.Instant;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder

@Table(name = "workspace_members" , indexes = {
        @Index(name = "idx_ws_member_lookup", columnList = "workspace_id, user_id", unique = true),
        @Index(name = "idx_user_ws_membership", columnList = "user_id") ,
        @Index(name = "idx_workspace_members", columnList = "workspace_id") ,
        @Index(name = "idx_ws_member_status", columnList = "workspace_id, status")
})
@Entity
public class WorkspaceMember extends BaseEntity{

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workspace_id", nullable = false, updatable = false)
    private Workspace workspace;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, updatable = false)
    private User user;

    //OWNER > ADMIN > EDITOR > VIEWER
    @Enumerated(EnumType.STRING)
    @Column(name = "role" , nullable = false)
    @Builder.Default
    private WorkspaceMemberRole role = WorkspaceMemberRole.VIEWER;

    @Enumerated(EnumType.STRING)
    @Column(name = "status" , nullable = false)
    @Builder.Default
    private WorkspaceMemberStatus status = WorkspaceMemberStatus.PENDING ;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invited_by_id" , updatable = false)
    private User invitedBy ;

    @Column(name = "invited_at" , nullable = false)
    @CreationTimestamp
    private Instant invitedAt ;

    @Column(name = "joined_at")
    private Instant joinedAt ;

    @SoftDelete(strategy = SoftDeleteType.TIMESTAMP)
    @Column(name = "removed_at")
    private Instant removedAt ;
}

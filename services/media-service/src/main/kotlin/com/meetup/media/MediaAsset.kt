package com.meetup.media

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.springframework.data.jpa.repository.JpaRepository
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "media_assets")
class MediaAsset(
    @Id
    val id: UUID = UUID.randomUUID(),
    @Column(nullable = false)
    val meetupId: UUID,
    @Column(nullable = false)
    val uploadedBy: UUID,
    @Column(nullable = false)
    val fileName: String,
    @Column(nullable = false)
    val contentType: String,
    @Column(nullable = false)
    val sizeBytes: Long,
    /** Path relative to the storage dir. In production: an S3 object key. */
    @Column(nullable = false)
    val storageKey: String,
    val uploadedAt: Instant = Instant.now(),
)

interface MediaAssetRepository : JpaRepository<MediaAsset, UUID> {
    fun findByMeetupIdOrderByUploadedAtDesc(meetupId: UUID): List<MediaAsset>
}

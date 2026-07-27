package com.meetup.media

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.springframework.data.jpa.repository.JpaRepository
import java.time.Instant
import java.util.UUID

/**
 * Метаданные загруженного файла (сами байты лежат в хранилище).
 *
 * @property id идентификатор файла.
 * @property meetupId встреча, к альбому которой относится файл.
 * @property uploadedBy пользователь, загрузивший файл.
 * @property fileName исходное имя файла.
 * @property contentType MIME-тип содержимого.
 * @property sizeBytes размер файла в байтах.
 * @property storageKey путь относительно каталога хранения; в проде — ключ объекта S3.
 * @property uploadedAt момент загрузки.
 */
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
    @Column(nullable = false)
    val storageKey: String,
    val uploadedAt: Instant = Instant.now(),
)

/** Репозиторий метаданных файлов. */
interface MediaAssetRepository : JpaRepository<MediaAsset, UUID> {
    /** Альбом встречи: файлы в порядке от новых к старым. */
    fun findByMeetupIdOrderByUploadedAtDesc(meetupId: UUID): List<MediaAsset>
}

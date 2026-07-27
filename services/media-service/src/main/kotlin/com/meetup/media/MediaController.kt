package com.meetup.media

import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.FileSystemResource
import org.springframework.core.io.Resource
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile
import org.springframework.web.server.ResponseStatusException
import java.nio.file.Files
import java.nio.file.Path
import java.util.UUID

/**
 * HTTP-эндпоинты альбомов встреч: загрузка, список, скачивание.
 *
 * Для разработки байты хранятся на локальном диске; в продакшене сервис
 * выдаёт presigned-ссылки S3, не пропуская содержимое через JVM.
 *
 * @property assets репозиторий метаданных файлов.
 * @param storageDir каталог хранения файлов из конфигурации.
 */
@RestController
@RequestMapping("/media")
class MediaController(
    private val assets: MediaAssetRepository,
    @Value("\${media.storage-dir}") storageDir: String,
) {
    /** Абсолютный корень каталога хранения; создаётся при старте. */
    private val root: Path = Path.of(storageDir).toAbsolutePath()

    init {
        Files.createDirectories(root)
    }

    /**
     * Загружает файл в альбом встречи [meetupId] от имени текущего пользователя.
     *
     * @throws ResponseStatusException 400, если файл пустой.
     */
    @PostMapping("/meetups/{meetupId}")
    @ResponseStatus(HttpStatus.CREATED)
    fun upload(
        @PathVariable meetupId: UUID,
        @RequestHeader("X-User-Id") userId: UUID,
        @RequestParam("file") file: MultipartFile,
    ): MediaAsset {
        if (file.isEmpty) throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Empty file")
        val assetId = UUID.randomUUID()
        val storageKey = "$meetupId/$assetId"
        val target = root.resolve(storageKey)
        Files.createDirectories(target.parent)
        file.inputStream.use { Files.copy(it, target) }
        return assets.save(
            MediaAsset(
                id = assetId,
                meetupId = meetupId,
                uploadedBy = userId,
                fileName = file.originalFilename ?: "upload",
                contentType = file.contentType ?: MediaType.APPLICATION_OCTET_STREAM_VALUE,
                sizeBytes = file.size,
                storageKey = storageKey,
            ),
        )
    }

    /** Альбом встречи: метаданные файлов от новых к старым. */
    @GetMapping("/meetups/{meetupId}")
    fun album(@PathVariable meetupId: UUID): List<MediaAsset> =
        assets.findByMeetupIdOrderByUploadedAtDesc(meetupId)

    /**
     * Отдаёт содержимое файла с корректным Content-Type.
     *
     * @throws ResponseStatusException 404 — метаданные не найдены;
     * 410 — файл пропал из хранилища.
     */
    @GetMapping("/{assetId}/content")
    fun download(@PathVariable assetId: UUID): ResponseEntity<Resource> {
        val asset = assets.findById(assetId).orElseThrow {
            ResponseStatusException(HttpStatus.NOT_FOUND, "Asset not found")
        }
        val file = root.resolve(asset.storageKey)
        if (!Files.exists(file)) {
            throw ResponseStatusException(HttpStatus.GONE, "File is missing from storage")
        }
        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(asset.contentType))
            .body(FileSystemResource(file))
    }
}

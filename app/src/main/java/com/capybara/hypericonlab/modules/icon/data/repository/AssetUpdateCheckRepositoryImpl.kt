package com.capybara.hypericonlab.modules.icon.data.repository

import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import com.capybara.hypericonlab.modules.icon.domain.lawnicons.AssetUpdateCheckState
import com.capybara.hypericonlab.modules.icon.domain.lawnicons.FailureReason
import com.capybara.hypericonlab.modules.icon.domain.lawnicons.LawniconsVersion
import com.capybara.hypericonlab.modules.icon.domain.lawnicons.ReleaseAssetInfo
import com.capybara.hypericonlab.modules.icon.domain.lawnicons.ReleaseInfo
import com.capybara.hypericonlab.modules.icon.domain.lawnicons.ResourceSource
import com.capybara.hypericonlab.modules.icon.domain.repository.AssetUpdateCheckRecord
import com.capybara.hypericonlab.modules.icon.domain.repository.AssetUpdateCheckRepository
import com.capybara.hypericonlab.modules.settings.data.local.AppDataStore
import kotlinx.coroutines.flow.first
import org.json.JSONObject

class AssetUpdateCheckRepositoryImpl(
    private val appDataStore: AppDataStore
) : AssetUpdateCheckRepository {

    override suspend fun read(): AssetUpdateCheckRecord {
        val preferences = appDataStore.data.first()
        return AssetUpdateCheckRecord(
            lastAutomaticCheckAt = preferences[AppDataStore.ASSET_CHECK_LAST_AUTOMATIC_AT],
            lastManualCheckAt = preferences[AppDataStore.ASSET_CHECK_LAST_MANUAL_AT],
            state = decodeState(preferences[AppDataStore.ASSET_CHECK_STATE])
        )
    }

    override suspend fun save(record: AssetUpdateCheckRecord) {
        appDataStore.edit { preferences ->
            putOptionalLong(
                preferences,
                AppDataStore.ASSET_CHECK_LAST_AUTOMATIC_AT,
                record.lastAutomaticCheckAt
            )
            putOptionalLong(
                preferences,
                AppDataStore.ASSET_CHECK_LAST_MANUAL_AT,
                record.lastManualCheckAt
            )
            preferences[AppDataStore.ASSET_CHECK_STATE] = encodeState(record.state)
        }
    }

    private fun encodeState(state: AssetUpdateCheckState): String {
        val json = JSONObject()
        when (state) {
            AssetUpdateCheckState.Idle -> json.put(TYPE_KEY, TYPE_IDLE)
            is AssetUpdateCheckState.Checking -> json.put(TYPE_KEY, TYPE_IDLE)
            is AssetUpdateCheckState.UpToDate -> {
                json.put(TYPE_KEY, TYPE_UP_TO_DATE)
                json.put(CURRENT_VERSION_KEY, state.currentVersion)
            }

            is AssetUpdateCheckState.Available -> {
                json.put(TYPE_KEY, TYPE_AVAILABLE)
                json.put(VERSION_INFO_KEY, encodeVersion(state.currentVersion))
                json.put(RELEASE_INFO_KEY, encodeRelease(state.availableRelease))
                json.put(RESOURCE_REQUIRED_KEY, state.resourceUpdateRequired)
                json.put(TEMPLATE_REQUIRED_KEY, state.templateUpdateRequired)
                json.put(CACHE_REQUIRED_KEY, state.cacheRebuildRequired)
            }

            is AssetUpdateCheckState.Failed -> {
                json.put(TYPE_KEY, TYPE_FAILED)
                json.put(REASON_KEY, state.reason.name)
            }
        }
        return json.toString()
    }

    private fun decodeState(value: String?): AssetUpdateCheckState {
        if (value.isNullOrBlank()) return AssetUpdateCheckState.Idle
        return runCatching {
            val json = JSONObject(value)
            when (json.optString(TYPE_KEY)) {
                TYPE_UP_TO_DATE -> AssetUpdateCheckState.UpToDate(
                    currentVersion = json.optString(CURRENT_VERSION_KEY)
                )

                TYPE_AVAILABLE -> AssetUpdateCheckState.Available(
                    currentVersion = decodeVersion(json.getJSONObject(VERSION_INFO_KEY)),
                    availableRelease = decodeRelease(json.getJSONObject(RELEASE_INFO_KEY)),
                    resourceUpdateRequired = json.optBoolean(RESOURCE_REQUIRED_KEY),
                    templateUpdateRequired = json.optBoolean(TEMPLATE_REQUIRED_KEY),
                    cacheRebuildRequired = json.optBoolean(CACHE_REQUIRED_KEY)
                )

                TYPE_FAILED -> AssetUpdateCheckState.Failed(
                    reason = enumValueOrDefault(
                        json.optString(REASON_KEY),
                        FailureReason.UNKNOWN
                    )
                )

                else -> AssetUpdateCheckState.Idle
            }
        }.getOrDefault(AssetUpdateCheckState.Idle)
    }

    private fun encodeVersion(version: LawniconsVersion): JSONObject = JSONObject().apply {
        put(VERSION_KEY, version.version)
        put(SOURCE_KEY, version.source.name)
        put(COMMIT_KEY, version.lawniconsCommit)
        put(GENERATED_AT_KEY, version.generatedAt)
        put(SVG_COUNT_KEY, version.svgCount)
        put(MAPPER_COUNT_KEY, version.mapperCount)
    }

    private fun decodeVersion(json: JSONObject): LawniconsVersion = LawniconsVersion(
        version = json.optString(VERSION_KEY),
        source = enumValueOrDefault(json.optString(SOURCE_KEY), ResourceSource.ASSETS),
        lawniconsCommit = json.optString(COMMIT_KEY),
        generatedAt = json.optString(GENERATED_AT_KEY),
        svgCount = json.optInt(SVG_COUNT_KEY),
        mapperCount = json.optInt(MAPPER_COUNT_KEY)
    )

    private fun encodeRelease(release: ReleaseInfo): JSONObject = JSONObject().apply {
        put(VERSION_KEY, release.version)
        put(ZIP_URL_KEY, release.zipUrl)
        put(ZIP_SIZE_KEY, release.zipSizeBytes)
        put(SHA256_KEY, release.sha256)
        put(COMMIT_KEY, release.lawniconsCommit)
        put(GENERATED_AT_KEY, release.generatedAt)
        put(TOTAL_ICONS_KEY, release.totalIcons)
        put(ADDED_ICONS_KEY, release.addedIcons)
        put(REMOVED_ICONS_KEY, release.removedIcons)
        put(MODIFIED_ICONS_KEY, release.modifiedIcons)
        release.templateArchive?.let { put(TEMPLATE_ARCHIVE_KEY, encodeTemplateArchive(it)) }
    }

    private fun decodeRelease(json: JSONObject): ReleaseInfo = ReleaseInfo(
        version = json.optString(VERSION_KEY),
        zipUrl = json.optString(ZIP_URL_KEY),
        zipSizeBytes = json.optLong(ZIP_SIZE_KEY),
        sha256 = json.optString(SHA256_KEY),
        lawniconsCommit = json.optString(COMMIT_KEY),
        generatedAt = json.optString(GENERATED_AT_KEY),
        totalIcons = json.optInt(TOTAL_ICONS_KEY),
        addedIcons = json.optInt(ADDED_ICONS_KEY),
        removedIcons = json.optInt(REMOVED_ICONS_KEY),
        modifiedIcons = json.optInt(MODIFIED_ICONS_KEY),
        templateArchive = json.optJSONObject(TEMPLATE_ARCHIVE_KEY)?.let(::decodeTemplateArchive)
    )

    private fun encodeTemplateArchive(archive: ReleaseAssetInfo): JSONObject = JSONObject().apply {
        put(ZIP_URL_KEY, archive.url)
        put(ZIP_SIZE_KEY, archive.sizeBytes)
    }

    private fun decodeTemplateArchive(json: JSONObject): ReleaseAssetInfo = ReleaseAssetInfo(
        url = json.optString(ZIP_URL_KEY),
        sizeBytes = json.optLong(ZIP_SIZE_KEY)
    )

    private inline fun <reified T : Enum<T>> enumValueOrDefault(value: String, default: T): T =
        runCatching { enumValueOf<T>(value) }.getOrDefault(default)

    private fun putOptionalLong(
        preferences: MutablePreferences,
        key: Preferences.Key<Long>,
        value: Long?
    ) {
        if (value == null) preferences.remove(key) else preferences[key] = value
    }

    private companion object {
        const val TYPE_KEY = "type"
        const val TYPE_IDLE = "idle"
        const val TYPE_UP_TO_DATE = "up_to_date"
        const val TYPE_AVAILABLE = "available"
        const val TYPE_FAILED = "failed"
        const val CURRENT_VERSION_KEY = "current_version"
        const val VERSION_INFO_KEY = "version_info"
        const val RELEASE_INFO_KEY = "release_info"
        const val RESOURCE_REQUIRED_KEY = "resource_required"
        const val TEMPLATE_REQUIRED_KEY = "template_required"
        const val CACHE_REQUIRED_KEY = "cache_required"
        const val REASON_KEY = "reason"
        const val VERSION_KEY = "version"
        const val SOURCE_KEY = "source"
        const val COMMIT_KEY = "commit"
        const val GENERATED_AT_KEY = "generated_at"
        const val SVG_COUNT_KEY = "svg_count"
        const val MAPPER_COUNT_KEY = "mapper_count"
        const val ZIP_URL_KEY = "zip_url"
        const val ZIP_SIZE_KEY = "zip_size"
        const val SHA256_KEY = "sha256"
        const val TOTAL_ICONS_KEY = "total_icons"
        const val ADDED_ICONS_KEY = "added_icons"
        const val REMOVED_ICONS_KEY = "removed_icons"
        const val MODIFIED_ICONS_KEY = "modified_icons"
        const val TEMPLATE_ARCHIVE_KEY = "template_archive"
    }
}

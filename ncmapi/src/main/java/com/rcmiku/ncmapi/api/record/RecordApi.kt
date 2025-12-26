package com.rcmiku.ncmapi.api.record

import com.rcmiku.ncmapi.model.RecordResponse
import com.rcmiku.ncmapi.model.SongRecordType

object RecordApi {
    suspend fun songRecord(uid: Long, type: SongRecordType = SongRecordType.WEEK): Result<RecordResponse> {
        return Result.failure(NotImplementedError())
    }
}

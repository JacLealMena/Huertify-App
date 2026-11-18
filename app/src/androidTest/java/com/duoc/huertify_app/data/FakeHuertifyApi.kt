package com.duoc.huertify_app.data

import com.duoc.huertify_app.data.local.Crop
import com.duoc.huertify_app.data.local.Sector
import com.duoc.huertify_app.data.local.Task
import com.duoc.huertify_app.data.remote.HuertifyApi
import com.duoc.huertify_app.data.remote.LoginRequest
import com.duoc.huertify_app.data.remote.UserResponse

class FakeHuertifyApi : HuertifyApi {
    private var nextSectorId = 5L
    private var nextCropId = 5L
    private var nextTaskId = 8L

    override suspend fun login(request: LoginRequest): UserResponse = error("Not used in this test")
    override suspend fun getUsers(): List<UserResponse> = error("Not used in this test")
    override suspend fun getSectors(): List<Sector> = error("Not used in this test")

    override suspend fun createSector(sector: Sector): Sector =
        sector.copy(id = nextSectorId++)

    override suspend fun updateSector(id: Long, sector: Sector): Sector = sector.copy(id = id)
    override suspend fun deleteSector(id: Long) = Unit
    override suspend fun getCrops(sectorId: Long?): List<Crop> = error("Not used in this test")

    override suspend fun createCrop(crop: Crop): Crop =
        crop.copy(id = nextCropId++)

    override suspend fun updateCrop(id: Long, crop: Crop): Crop = crop.copy(id = id)
    override suspend fun deleteCrop(id: Long) = Unit
    override suspend fun getTasks(assigneeId: Long?): List<Task> = error("Not used in this test")

    override suspend fun createTask(task: Task): Task =
        task.copy(id = nextTaskId++)

    override suspend fun updateTask(id: Long, task: Task): Task = task.copy(id = id)
    override suspend fun deleteTask(id: Long) = Unit
}

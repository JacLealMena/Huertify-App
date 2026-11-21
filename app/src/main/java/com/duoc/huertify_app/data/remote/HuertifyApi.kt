package com.duoc.huertify_app.data.remote

import com.duoc.huertify_app.data.local.Crop
import com.duoc.huertify_app.data.local.Sector
import com.duoc.huertify_app.data.local.Task
import com.duoc.huertify_app.domain.model.Role
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface HuertifyApi {
    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequest): UserResponse

    @GET("api/users")
    suspend fun getUsers(): List<UserResponse>

    @GET("api/sectors")
    suspend fun getSectors(): List<Sector>

    @POST("api/sectors")
    suspend fun createSector(@Body sector: Sector): Sector

    @PUT("api/sectors/{id}")
    suspend fun updateSector(@Path("id") id: Long, @Body sector: Sector): Sector

    @DELETE("api/sectors/{id}")
    suspend fun deleteSector(@Path("id") id: Long)

    @GET("api/crops")
    suspend fun getCrops(@Query("sectorId") sectorId: Long? = null): List<Crop>

    @POST("api/crops")
    suspend fun createCrop(@Body crop: Crop): Crop

    @PUT("api/crops/{id}")
    suspend fun updateCrop(@Path("id") id: Long, @Body crop: Crop): Crop

    @DELETE("api/crops/{id}")
    suspend fun deleteCrop(@Path("id") id: Long)

    @GET("api/tasks")
    suspend fun getTasks(@Query("assigneeId") assigneeId: Long? = null): List<Task>

    @POST("api/tasks")
    suspend fun createTask(@Body task: Task): Task

    @PUT("api/tasks/{id}")
    suspend fun updateTask(@Path("id") id: Long, @Body task: Task): Task

    @DELETE("api/tasks/{id}")
    suspend fun deleteTask(@Path("id") id: Long)
}

data class LoginRequest(val email: String, val password: String)

data class UserResponse(
    val id: Long,
    val name: String,
    val email: String,
    val role: Role,
    val workshop: String?
)

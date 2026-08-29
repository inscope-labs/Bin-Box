package com.inscopelabs.abx.binbox.oci.api

import com.inscopelabs.abx.binbox.oci.api.compartments.IdentityApi
import com.inscopelabs.abx.binbox.oci.api.compute.ComputeApi
import com.inscopelabs.abx.binbox.oci.api.compute.ImageApi
import com.inscopelabs.abx.binbox.oci.api.compute.ShapeApi
import com.inscopelabs.abx.binbox.oci.api.networking.InternetGatewayApi
import com.inscopelabs.abx.binbox.oci.api.networking.RouteTableApi
import com.inscopelabs.abx.binbox.oci.api.networking.SubnetApi
import com.inscopelabs.abx.binbox.oci.api.networking.VcnApi
import com.inscopelabs.abx.binbox.oci.api.networking.VnicApi
import com.inscopelabs.abx.binbox.oci.identity.OciCredentials
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

/**
 * Builds signed Retrofit clients against OCI's Identity and Core Services
 * hosts (see [OciApiConfig] for the confirmed host patterns). One instance
 * per active [region] — construct after credentials are known
 * (post `CONNECTION_VERIFICATION`), not as an app-wide singleton, since the
 * base URLs are region-specific and Retrofit fixes them at build time.
 *
 * Logging is set to [HttpLoggingInterceptor.Level.BASIC] — method, URL,
 * response code only — never [HttpLoggingInterceptor.Level.HEADERS] or
 * `BODY`. The signed `Authorization` header itself contains no private key
 * material (just keyId + a per-request signature, per §8's "never expose
 * private key material to logs"), but request/response bodies can contain
 * account-identifying data that doesn't belong in logs either.
 */
class OciClient(
    region: String,
    baseUrlOverride: String? = null,
    credentialsProvider: () -> OciCredentials?
) {
    constructor(region: String, credentialsProvider: () -> OciCredentials?) : this(region, null, credentialsProvider)

    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(OciSigningInterceptor(credentialsProvider))
        .addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC })
        .build()

    private fun retrofitFor(baseUrl: String): Retrofit = Retrofit.Builder()
        .baseUrl(baseUrl)
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    private val identityRetrofit = retrofitFor(baseUrlOverride ?: OciApiConfig.identityBaseUrl(region))
    private val coreServicesRetrofit = retrofitFor(baseUrlOverride ?: OciApiConfig.coreServicesBaseUrl(region))

    val identityApi: IdentityApi by lazy { identityRetrofit.create(IdentityApi::class.java) }
    val vcnApi: VcnApi by lazy { coreServicesRetrofit.create(VcnApi::class.java) }
    val subnetApi: SubnetApi by lazy { coreServicesRetrofit.create(SubnetApi::class.java) }
    val internetGatewayApi: InternetGatewayApi by lazy { coreServicesRetrofit.create(InternetGatewayApi::class.java) }
    val routeTableApi: RouteTableApi by lazy { coreServicesRetrofit.create(RouteTableApi::class.java) }
    val vnicApi: VnicApi by lazy { coreServicesRetrofit.create(VnicApi::class.java) }
    val computeApi: ComputeApi by lazy { coreServicesRetrofit.create(ComputeApi::class.java) }
    val shapeApi: ShapeApi by lazy { coreServicesRetrofit.create(ShapeApi::class.java) }
    val imageApi: ImageApi by lazy { coreServicesRetrofit.create(ImageApi::class.java) }
}

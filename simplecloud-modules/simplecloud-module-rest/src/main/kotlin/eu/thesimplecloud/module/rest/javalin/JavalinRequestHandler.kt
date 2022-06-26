/*
 * MIT License
 *
 * Copyright (C) 2020-2022 The SimpleCloud authors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software,
 * and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
 * INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 * IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE,
 * ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
 * DEALINGS IN THE SOFTWARE.
 */

package eu.thesimplecloud.module.rest.javalin

import com.auth0.jwt.interfaces.DecodedJWT
import eu.thesimplecloud.jsonlib.JsonLib
import eu.thesimplecloud.module.rest.auth.AuthService
import eu.thesimplecloud.module.rest.auth.JwtProvider
import eu.thesimplecloud.module.rest.auth.user.User
import eu.thesimplecloud.module.rest.controller.RequestMethodData
import eu.thesimplecloud.module.rest.defaultcontroller.dto.ErrorDto
import io.javalin.http.Context
import io.javalin.http.Handler
import javalinjwt.JavalinJWT
import java.lang.reflect.InvocationTargetException
import java.util.*

/**
 * Created by IntelliJ IDEA.
 * Date: 04.10.2020
 * Time: 18:02
 * @author Frederick Baier
 */
class JavalinRequestHandler(val requestMethodData: RequestMethodData, private val authService: AuthService) : Handler {

    override fun handle(ctx: Context) {
        val user = getUserByContext(ctx)

        if (!isPermitted(user)) {
            ctx.result("Unauthorized")
            ctx.status(401)
            return
        }

        try {
            SingleRequestProcessor(ctx, requestMethodData, user).processRequest()
        } catch (ex: Exception) {
            val exception = if (ex is InvocationTargetException) ex.cause!! else ex
            ctx.status(500)
            val json = JsonLib.fromObject(ErrorDto.fromException(exception))
            ctx.result(json.getAsJsonString())

            ex.printStackTrace()
        }
    }

    private fun isPermitted(user: User?): Boolean {
        return user?.hasPermission(this.requestMethodData.permission) ?: requestMethodData.permission.isEmpty()
    }

    private fun getUserByContext(context: Context): User? {
        val decodedJWTOptional: Optional<DecodedJWT> = JavalinJWT.getTokenFromHeader(context)
            .flatMap(JwtProvider.instance.provider::validateToken) as Optional<DecodedJWT>

        if (!decodedJWTOptional.isPresent) {
            return null
        }

        val decodedJWT = decodedJWTOptional.get()

        val username = decodedJWT.getClaim("username").asString()
        return authService.getUserByName(username)
    }

}
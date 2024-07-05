package fmi

import cats.effect.kernel.Resource
import cats.effect.{IO, IOApp}
import com.comcast.ip4s.ipv4
import com.typesafe.config.ConfigFactory
import fmi.infrastructure.TokenSignatureService
import fmi.infrastructure.db.DbModule
import fmi.club.ClubModule
import fmi.config.TennisAppConfig
import fmi.court.CourtModule
import fmi.reservation.ReservationModule
import fmi.user.UsersModule
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.implicits.*
import org.http4s.server.Server
import sttp.tapir.server.http4s.Http4sServerInterpreter
import sttp.tapir.swagger.bundle.SwaggerInterpreter

object TennisApp extends IOApp.Simple:
  val app: Resource[IO, Server] = for
    config <- Resource
      .eval(IO.blocking(ConfigFactory.load()))
      .map(_.getConfig("tennisApp"))
      .map(TennisAppConfig.fromConfig)

    tokenSignatureService = new TokenSignatureService(config.secretKey)

    dbModule <- DbModule(config.database)

    usersModule <- UsersModule(dbModule.dbTransactor, tokenSignatureService)
    clubModule <- ClubModule(dbModule.dbTransactor, usersModule.authenticationService)
    courtModule <- CourtModule(dbModule.dbTransactor, usersModule.authenticationService)
    reservationModule <- ReservationModule(dbModule.dbTransactor, usersModule.authenticationService)

    apiEndpoints = usersModule.endpoints ::: clubModule.endpoints ::: courtModule.endpoints ::: reservationModule.endpoints
    docEndpoints = SwaggerInterpreter().fromServerEndpoints[IO](apiEndpoints, "library-app", "1.0.0")

    routes = Http4sServerInterpreter[IO]().toRoutes(apiEndpoints ::: docEndpoints).orNotFound

    httpServer <- EmberServerBuilder
      .default[IO]
      .withHost(ipv4"0.0.0.0")
      .withPort(config.http.port)
      .withHttpApp(routes)
      .build
  yield httpServer

  def run: IO[Unit] =
    app
      .use(httpServer => IO.never)
      .onCancel(IO.println("Shutting down..."))

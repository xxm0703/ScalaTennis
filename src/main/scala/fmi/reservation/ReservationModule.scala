package fmi.reservation

import cats.effect.IO
import cats.effect.kernel.Resource
import fmi.infrastructure.db.DoobieDatabase.DbTransactor
import fmi.user.authentication. AuthenticationService
import sttp.tapir.server.ServerEndpoint

case class ReservationModule(
                              reservationDao: ReservationDao,
                              reservationService: ReservationService,
                              endpoints: List[ServerEndpoint[Any, IO]]
)

object ReservationModule:
  def apply(
    dbTransactor: DbTransactor,
    authenticationService: AuthenticationService
  ): Resource[IO, ReservationModule] =
    val reservationDao = new ReservationDao(dbTransactor)
    val reservationService = new ReservationService(dbTransactor)(reservationDao)
    val reservationController = new ReservationController(reservationService)(authenticationService)

    Resource.pure(
      ReservationModule(reservationDao, reservationService, reservationController.endpoints)
    )

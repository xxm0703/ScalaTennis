package fmi.reservation

import cats.effect.IO
import cats.effect.kernel.Resource
import fmi.infrastructure.db.DoobieDatabase.DbTransactor
import fmi.user.authentication. AuthenticationService
import sttp.tapir.server.ServerEndpoint
import fmi.notification.NotificationService

case class ReservationModule(
                              reservationDao: ReservationDao,
                              reservationService: ReservationService,
                              notificationService: NotificationService,
                              endpoints: List[ServerEndpoint[Any, IO]]
)

object ReservationModule:
  def apply(
    dbTransactor: DbTransactor,
    authenticationService: AuthenticationService,
    notificationService: NotificationService
  ): Resource[IO, ReservationModule] =
    val reservationDao = new ReservationDao(dbTransactor)
    val reservationService = new ReservationService(dbTransactor)(reservationDao)(notificationService)
    val reservationController = new ReservationController(reservationService)(authenticationService)

    Resource.pure(
      ReservationModule(reservationDao, reservationService,notificationService, reservationController.endpoints)
    )

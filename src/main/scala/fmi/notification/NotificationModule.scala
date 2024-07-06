package fmi.notification

import cats.effect.IO
import cats.effect.kernel.Resource
import fmi.infrastructure.db.DoobieDatabase.DbTransactor
import fmi.user.authentication. AuthenticationService
import sttp.tapir.server.ServerEndpoint

case class NotificationModule(
                               notificationDao: NotificationDao,
                               notificationService: NotificationService,
                               endpoints: List[ServerEndpoint[Any, IO]]
                            )

object NotificationModule:
  def apply(
             dbTransactor: DbTransactor,
             authenticationService: AuthenticationService
           ): Resource[IO, NotificationModule] =
    val notificationDao = new NotificationDao(dbTransactor)
    val notificationService = new NotificationService(dbTransactor)(notificationDao)
    val notificationController = new NotificationController(notificationService)(authenticationService)

    Resource.pure(
      NotificationModule(notificationDao, notificationService, notificationController.endpoints)
    )

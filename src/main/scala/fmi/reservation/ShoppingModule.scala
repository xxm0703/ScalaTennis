package fmi.tennis

import cats.effect.IO
import cats.effect.kernel.Resource
import fmi.infrastructure.db.DoobieDatabase.DbTransactor
import fmi.club.CourtAvailabilityDao
import fmi.user.authentication.{AuthenticatedUser, AuthenticationService}
import org.http4s.AuthedRoutes
import sttp.tapir.server.ServerEndpoint

case class TennisModule(
  orderDao: OrderDao,
  orderService: OrderService,
  endpoints: List[ServerEndpoint[Any, IO]]
)

object TennisModule:
  def apply(
    dbTransactor: DbTransactor,
    authenticationService: AuthenticationService,
    courtAvailabilityDao: CourtAvailabilityDao
  ): Resource[IO, TennisModule] =
    val orderDao = new OrderDao(dbTransactor)
    val orderService = new OrderService(dbTransactor)(courtAvailabilityDao, orderDao)
    val shippingController = new TennisController(orderService)(authenticationService)

    Resource.pure(
      TennisModule(orderDao, orderService, shippingController.endpoints)
    )

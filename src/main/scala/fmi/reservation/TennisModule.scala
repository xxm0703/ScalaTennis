package fmi.reservation

import cats.effect.IO
import cats.effect.kernel.Resource
import fmi.infrastructure.db.DoobieDatabase.DbTransactor
import fmi.user.authentication.AuthenticationService
import sttp.tapir.server.ServerEndpoint

case class TennisModule(
  orderDao: OrderDao,
  orderService: OrderService,
  endpoints: List[ServerEndpoint[Any, IO]]
)

object TennisModule:
  def apply(
    dbTransactor: DbTransactor,
    authenticationService: AuthenticationService): Resource[IO, TennisModule] =
    val orderDao = new OrderDao(dbTransactor)
    val orderService = new OrderService(dbTransactor)(orderDao)
    val shippingController = new TennisController(orderService)(authenticationService)

    Resource.pure(
      TennisModule(orderDao, orderService, shippingController.endpoints)
    )

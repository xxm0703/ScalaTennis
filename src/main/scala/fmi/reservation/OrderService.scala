package fmi.reservation

import cats.effect.IO
import cats.syntax.all.*
import doobie.implicits.*
import fmi.infrastructure.db.DoobieDatabase.DbTransactor
import fmi.user.UserId

class OrderService(dbTransactor: DbTransactor)(orderDao: OrderDao):
  // TODO: validate tennis cart has positive quantities
  def placeOrder(user: UserId, tennisCart: TennisCart): IO[Either[Unit, Order]] = for
    orderId <- OrderId.generate
    placingTimestamp <- IO.realTimeInstant

    order = Order(orderId, user, tennisCart.orderLines, placingTimestamp)

    maybeOrder <- transactOrder( order)
  yield maybeOrder

  private def transactOrder(
    order: Order
  ): IO[Either[Unit, Order]] =
    val transaction = orderDao.placeOrder(order)

    transaction
      .transact(dbTransactor)
      .map(_.asRight[Unit])
      .recover { case _ =>
        ().asLeft
      }

package fmi.shopping

import cats.effect.IO
import cats.syntax.all.*
import doobie.implicits.*
import fmi.infrastructure.db.DoobieDatabase.DbTransactor
import fmi.club.{ClubAdjustment, NotEnoughAvailabilityAvailable, NotEnoughAvailabilityAvailableException, CourtAvailabilityDao}
import fmi.user.UserId

class OrderService(dbTransactor: DbTransactor)(courtAvailabilityDao: CourtAvailabilityDao, orderDao: OrderDao):
  // TODO: validate shopping cart has positive quantities
  def placeOrder(user: UserId, shoppingCart: ShoppingCart): IO[Either[NotEnoughAvailabilityAvailable.type, Order]] = for
    orderId <- OrderId.generate
    placingTimestamp <- IO.realTimeInstant

    order = Order(orderId, user, shoppingCart.orderLines, placingTimestamp)

    maybeOrder <- transactOrder(shoppingCart.toClubAdjustment, order)
  yield maybeOrder

  private def transactOrder(
    clubAdjustment: ClubAdjustment,
    order: Order
  ): IO[Either[NotEnoughAvailabilityAvailable.type, Order]] =
    val transaction =
      courtAvailabilityDao.applyClubAdjustmentAction(clubAdjustment) *>
        orderDao.placeOrder(order)

    transaction
      .transact(dbTransactor)
      .map(_.asRight[NotEnoughAvailabilityAvailable.type])
      .recover { case NotEnoughAvailabilityAvailableException =>
        NotEnoughAvailabilityAvailable.asLeft
      }

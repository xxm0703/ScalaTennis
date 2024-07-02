package fmi.shopping

import fmi.club.ClubAdjustment
import io.circe.Codec
import sttp.tapir.Schema

case class ShoppingCart(orderLines: List[OrderLine] = List.empty) derives Codec, Schema:
  def add(orderLine: OrderLine): ShoppingCart =
    ShoppingCart(orderLine :: orderLines)

  def toClubAdjustment: ClubAdjustment =
    ClubAdjustment(orderLines.map(_.toCourtAvailabilityAdjustment))

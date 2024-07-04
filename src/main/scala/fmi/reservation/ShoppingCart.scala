package fmi.tennis

import fmi.club.ClubAdjustment
import io.circe.Codec
import sttp.tapir.Schema

case class TennisCart(orderLines: List[OrderLine] = List.empty) derives Codec, Schema:
  def add(orderLine: OrderLine): TennisCart =
    TennisCart(orderLine :: orderLines)

  def toClubAdjustment: ClubAdjustment =
    ClubAdjustment(orderLines.map(_.toCourtAvailabilityAdjustment))

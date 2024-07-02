package fmi.shopping

import cats.effect.IO
import fmi.club.{CourtId, CourtAvailabilityAdjustment}
import fmi.user.UserId
import fmi.utils.CirceUtils
import io.circe.Codec
import sttp.tapir.{Schema, SchemaType}

import java.time.Instant
import java.util.UUID

case class Order(orderId: OrderId, user: UserId, orderLines: List[OrderLine], placingTimestamp: Instant)
    derives Codec,
      Schema
case class OrderId(id: String)

object OrderId:
  def generate: IO[OrderId] = IO.randomUUID.map(uuid => OrderId.apply(uuid.toString))

  given Codec[OrderId] = CirceUtils.unwrappedCodec(OrderId.apply)(_.id)
  given Schema[OrderId] = Schema(SchemaType.SString())

case class OrderLine(court: CourtId, quantity: Int) derives Codec, Schema:
  def toCourtAvailabilityAdjustment = CourtAvailabilityAdjustment(court, -quantity)

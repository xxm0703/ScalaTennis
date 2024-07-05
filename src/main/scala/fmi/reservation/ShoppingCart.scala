package fmi.reservation

import io.circe.Codec
import sttp.tapir.Schema

case class TennisCart(orderLines: List[OrderLine] = List.empty) derives Codec, Schema:
  def add(orderLine: OrderLine): TennisCart =
    TennisCart(orderLine :: orderLines)

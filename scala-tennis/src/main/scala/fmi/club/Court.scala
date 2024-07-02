package fmi.club

import fmi.utils.CirceUtils
import io.circe.Codec
import sttp.tapir
import sttp.tapir.{CodecFormat, Schema, SchemaType}

case class CourtId(id: String)

object CourtId:
  given Codec[CourtId] = CirceUtils.unwrappedCodec(CourtId.apply)(_.id)
  given Schema[CourtId] = Schema(SchemaType.SString())
  given tapir.Codec[String, CourtId, CodecFormat.TextPlain] = tapir.Codec.string.map(CourtId.apply)(_.id)

case class Court(id: CourtId, name: String, description: String, weightInGrams: Int) derives Codec, Schema

package ch.seibertec.iot.converter

import java.nio.charset.Charset
import java.util
import java.util.Collections

import com.datamountaineer.streamreactor.connect.converters.MsgKey
import com.datamountaineer.streamreactor.connect.converters.source.{Converter, KeyExtractor}
import org.apache.kafka.connect.data._
import org.apache.kafka.connect.source.SourceRecord


class MqttConverter extends Converter {
  override def convert(kafkaTopic: String,
                       sourceTopic: String,
                       messageId: String,
                       bytes: Array[Byte],
                       keys:Seq[String] = Seq.empty,
                       keyDelimiter:String = "."): SourceRecord = {
    require(bytes != null, s"Invalid $bytes parameter")
    val json = new String(bytes, Charset.defaultCharset)
    val schemaAndValue = MqttConverter.convert(sourceTopic, json)
    val value = schemaAndValue.value()

    value match {
      case s:Struct if keys.nonEmpty =>
        val keysValue = keys.flatMap { key =>
          Option(KeyExtractor.extract(s, key.split('.').toVector)).map(_.toString)
        }.mkString(keyDelimiter)

        new SourceRecord(Collections.singletonMap(Converter.TopicKey, sourceTopic),
          null,
          kafkaTopic,
          Schema.STRING_SCHEMA,
          keysValue,
          schemaAndValue.schema(),
          schemaAndValue.value())
      case _=>
        new SourceRecord(Collections.singletonMap(Converter.TopicKey, sourceTopic),
          null,
          kafkaTopic,
          MsgKey.schema,
          MsgKey.getStruct(sourceTopic, messageId),
          schemaAndValue.schema(),
          schemaAndValue.value())
    }

  }
}

object MqttConverter {

  import org.json4s._
  import org.json4s.native.JsonMethods._

  def convert(name: String, str: String): SchemaAndValue = {
    val originalMessage = parse(str)
    val newMessage = originalMessage match {
      case JObject(fs) =>
        JObject(("SourceTopic", JString(name)) :: fs)
      case m => m
    }
    convert(name, newMessage)
  }

  def convert(name: String, value: JValue): SchemaAndValue = {
    value match {
      case JArray(arr) =>
        val values = new util.ArrayList[AnyRef]()
        val sv = convert(name, arr.head)
        values.add(sv.value())
        arr.tail.foreach { v => values.add(convert(name, v).value()) }

        val schema = SchemaBuilder.array(sv.schema()).optional().build()
        new SchemaAndValue(schema, values)
      case JBool(b) => new SchemaAndValue(Schema.BOOLEAN_SCHEMA, b)
      case JDecimal(d) =>
        val schema = Decimal.builder(d.scale).optional().build()
        new SchemaAndValue(schema, Decimal.fromLogical(schema, d.bigDecimal))
      case JDouble(d) => new SchemaAndValue(Schema.FLOAT64_SCHEMA, d)
      case JInt(i) => new SchemaAndValue(Schema.INT64_SCHEMA, i.toLong) //on purpose! LONG (we might get later records with long entries)
      case JLong(l) => new SchemaAndValue(Schema.INT64_SCHEMA, l)
      case JNull | JNothing => new SchemaAndValue(Schema.STRING_SCHEMA, null)
      case JString(s) => new SchemaAndValue(Schema.STRING_SCHEMA, s)
      case JObject(values) =>
        val builder = SchemaBuilder.struct().name(name.replace("/", "_"))

        val fields = values.map { case (n, v) =>
          val schemaAndValue = convert(n, v)
          builder.field(n, schemaAndValue.schema())
          n -> schemaAndValue.value()
        }.toMap
        val schema = builder.build()

        val struct = new Struct(schema)
        fields.foreach { case (field, v) => struct.put(field, v) }

        new SchemaAndValue(schema, struct)
    }
  }
}
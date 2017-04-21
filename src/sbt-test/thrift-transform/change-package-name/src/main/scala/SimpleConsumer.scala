// consume the generated case class, this will only work if it has
// been modified to live in the correct package

// defined in the thrift as 'prefix.rest'
import modified.rest._

class SimpleConsumer {
  val x1 = SimpleStruct(name = None, age = Some(10))
  val x2 = OtherSimpleStruct(number = Some(5.6))
  val e1: SimpleEnum = SimpleEnum.Yes
}

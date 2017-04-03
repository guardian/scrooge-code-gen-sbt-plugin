// consume the generated case class
//

import testpackage._

class SimpleConsumer {
  val x1 = SimpleStruct(name = None, age = Some(10))
  val x2 = OtherSimpleStruct(number = Some(5.6))
  val e1: SimpleEnum = SimpleEnum.yes
}

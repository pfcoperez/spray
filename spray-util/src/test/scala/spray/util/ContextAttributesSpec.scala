package spray.util

import org.specs2.mutable.Specification

class ContextAttributesSpec extends Specification {

  "ContextAttributes" should {
    "should be able to store values" in {
      val empty = ContextAttributes.empty
      empty.isEmpty === true

      val withInteger = empty.updated(42)
      withInteger.isEmpty === false
      withInteger.get[Int] === Some(42)

      val strExample: String = "meaning of life universe and everything"

      val withStringAndInteger = withInteger.updated(strExample)
      withStringAndInteger.get[Int] === Some(42)
      withStringAndInteger.get[String] === Some(strExample)
    }

    "should be able to store parametrized types" in {
      val empty = ContextAttributes.empty
      empty.isEmpty === true

      val intList: List[Int] = List(42)
      val withIntegerList = empty.updated(intList)
      withIntegerList.isEmpty === false
      withIntegerList.get[List[Int]] === Some(intList)

      val strList: List[String] = List("meaning of life universe and everything")
      val withStringAndInteger = withIntegerList.updated(strList)
      withStringAndInteger.get[List[Int]] === Some(intList)
      withStringAndInteger.get[List[String]] === Some(strList)
    }
  }

}
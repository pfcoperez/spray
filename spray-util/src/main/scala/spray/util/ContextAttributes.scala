package spray.util

import scala.reflect.runtime.universe.TypeTag

/**
  * Class providing type keyed map used in contexts such as marshalling and request contexts.
  */
class ContextAttributes private (private val underlying: Map[TypeTag[_], Any]) extends AnyVal {

  /**
    * Provides a copy of the map with the value `value` at key `t`
    * @param t Type tag used as key to store the new value.
    * @param value Valued stored as the entry for `t`
    * @return A new map with the entry (`t`, `value`)
    */
  def updated[T](value: T)(implicit t: TypeTag[T]): ContextAttributes =
    new ContextAttributes(underlying.updated(t, value))

  /**
    * Provides the element for key `t` if present in the map, `None` otherwise.
    *
    * @param t Query key as a [[TypeTag]]
    * @return The stored value for `t` wrapped by [[Some]] if present, [[None]] otherwise
    */
  def get[T](implicit t: TypeTag[T]): Option[T] = underlying.get(t).map(_.asInstanceOf[T])

  def isEmpty: Boolean = underlying.isEmpty
}

object ContextAttributes {
  val empty: ContextAttributes = new ContextAttributes(Map.empty)
}

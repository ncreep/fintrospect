package io.github.daviddenton.fintrospect.parameters

import scala.reflect.ClassTag

abstract class PathParameter[T](name: String, description: Option[String])(implicit ct: ClassTag[T])
  extends Parameter[T, String](name, description, "path")(ct)
  with Iterable[PathParameter[_]] {
}

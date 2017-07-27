// Copyright 2017 Google Inc.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package sync3k.routes

import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.directives.PathDirectives.pathEndOrSingleSlash
import akka.http.scaladsl.server.directives.RouteDirectives.complete

/**
 * Routes can be defined in separated classes like shown in here
 */
object BaseRoutes {

  // This route is the one that listens to the top level '/'
  lazy val baseRoutes: Route =
    pathEndOrSingleSlash { // Listens to the top `/`
      complete("Server up and running") // Completes with some text
    }
}

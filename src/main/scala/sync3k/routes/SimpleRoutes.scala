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

import akka.http.scaladsl.marshallers.xml.ScalaXmlSupport.defaultNodeSeqMarshaller
import akka.http.scaladsl.server.directives.MethodDirectives.get
import akka.http.scaladsl.server.directives.PathDirectives.path
import akka.http.scaladsl.server.directives.RouteDirectives.complete

/**
 * Routes can be defined in separated classes like shown in here
 */
trait SimpleRoutes {

  // This `val` holds one route (of possibly many more that will be part of your Web App)
  lazy val simpleRoutes =
    path("hello") { // Listens to paths that are exactly `/hello`
      get { // Listens only to GET requests
        complete(<html><body><h1>Say hello to akka-http</h1></body></html>) // Completes with some html page
      }
    }
}

(*
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *)

(*
 * Run this script once per machine with "wolframscript -file init_daner_script.w" to download the models used
 * by the script. This can take several minutes, but the models will be cached "forever" after that.
 *
 * If this script is run a second time on the same machine, it will delete the downloaded models and fetch them again.
 *)

DeleteObject[ResourceObject["Wolfram Face Discriminator V2"]];
DeleteObject[ResourceObject["ResNet-101 Trained on Augmented CASIA-WebFace Data"]];

featuresFile = "extracted-features-2021-03-22T13.59.09.mx";

<<src/main/resources/DANER.m

findSimilarFaces["https://thispersondoesnotexist.com/image","JPG",2]

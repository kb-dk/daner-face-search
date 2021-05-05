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

(* :Title: DANER *)
(* :Context: DANER` *)
(* :Author: pmd *)
(* :Date: 2021-05-03 *)

logString[s_String] := Module[
  {len = StringLength@s},
  "##"
      <> StringRepeat[" ", 35 - Floor[len/2]]
      <> s
      <> StringRepeat[" ", 35 - Ceiling[len/2]]
      <> "##"
];

log[s_String] := Write["stderr", DateString["ISODateTime"]<>" - "<>s];

(* CONFIGURATION *)
(* featuresFile must be set before calling this script *)
(* featuresFile = "src/main/resources/extracted-features-2021-03-22T13.59.09.mx"; *)
(* END OF CONFIGURATION *)

Write["stderr", StringRepeat["#",74]]
Write["stderr", logString@""]
Write["stderr", logString@"** BEGIN PROGRAM: FACE SEARCH WEB SERVER **"]
Write["stderr", logString[DateString[]]]
Write["stderr", logString@""]
Write["stderr", logString@"CONFIGURATION"]
Write["stderr", logString@""]
Write["stderr", "## Load features from "<>featuresFile<>"."]

Write["stderr",logString@""]
Write["stderr", StringRepeat["#",74]]

(* BEGIN PROGRAM *)

Write["stderr", "## Load and initialize ANN data"];
Write["stderr", "## CWD: "<>Directory[]];
Write["stderr", "## Load precalculated features from "<>featuresFile];
Write["stderr", "## Does the file exist: "<>ToString@FileExistsQ@featuresFile];
Write["stderr", "## File size: "<>ToString@FileSize@featuresFile];

Get@featuresFile;
Write["stderr", "Symbols: "<>ToString@Names["Global`*"]];
Write["stderr", "## Dimensions of loaded data: "<>ToString@Dimensions@faceFeatures];

features = faceFeatures[[All,2]];
imagePaths = faceFeatures[[All,1]];
Write["stderr", "## Loaded 2048 dimensional features from "<>ToString@Length@features<>" images."];

featureExtractor = NetModel["ResNet-101 Trained on Augmented CASIA-WebFace Data"];
findNearestPhoto = FeatureNearest[features -> {"Index", "Distance"}];
Write["stderr", "## Created a findNearest function giving index and distance"]
Write["stderr", "## All data loaded and initialized"];

findSimilarFaces[testImagePath_String, imageType_String, n_Integer] := Module[
  {image, faces, transformedFace, nearestFaces},

  byteArray = ReadByteArray[testImagePath];
  log["## loaded "<>ToString@Length@byteArray<>" bytes from "<>testImagePath];

  Catch[image = ImportByteArray[byteArray, imageType]];
  log["## Convert bytes to image: "<>ToString@Head@image];

  If[Not[Head@image === Image],
    log[logString@"!! ERROR !!"];
    log["## Unable to process image"];
    ExportString[<|"error"->"Unable to import PNG file."|>,"JSON"],


    log["## ImageDimensions: "<>ToString@ImageDimensions[image]];
    log["## Look for faces"];
    faces = FaceAlign[image, Automatic, {224,224}];
    log["## Found "<>ToString@Length@faces<>" faces in image"];
    If[Length @ faces == 0,
      ExportString[<| "error" -> "Found no face" |>, "JSON"],

      log["## Extract features from the B/W version of all the found faces, and find the nearest "<>ToString@n<>" faces of those."];

      nearestFaces = findNearestPhoto[featureExtractor@ColorConvert[#, "Grayscale"], n]&/@faces;

      log["## Return a JSON representation"];

      ExportString[
        Map[
          <|
            "id" -> FileBaseName@FileNameTake@imagePaths[[#[[1]]]],
            "distance" -> #[[2]]
          |> &,
          #
        ] & /@ nearestFaces,
        "JSON"
      ]
    ]
  ]
]

Write["stderr", "## DANER loaded and ready for requests."]
(* result = resultJSON[imageUrl,3];*)



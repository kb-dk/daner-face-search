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

(*
 0: ERROR
 1: info
 2: verbose
*)
logLevel = 2;

log[level_Integer, s_String] := If[level<=logLevel,
  Write["stderr", DateString["ISODateTime"]<>" - "<>s]
];

(* CONFIGURATION *)
(* featuresFile must be set before calling this script *)
(* featuresFile = "src/main/resources/extracted-features-2021-03-22T13.59.09.mx"; *)
(* END OF CONFIGURATION *)

(* BEGIN PROGRAM *)
log[2,"## Load and initialize ANN data"];
log[2,"## CWD: "<>Directory[]];
log[1,"## Load precalculated features from "<>featuresFile];
log[1,"## Does the file exist: "<>ToString@FileExistsQ@featuresFile];
If[Not@FileExistsQ@featuresFile,
  log[0, "features file not found"];
  Abort[];
];

log[2, "## File size: "<>ToString@FileSize@featuresFile];
Get@featuresFile;

log[2, "## Dimensions of loaded data: "<>ToString@Dimensions@faceFeatures];

features = faceFeatures[[All,2]];
imagePaths = faceFeatures[[All,1]];
log[1, "## Loaded 2048 dimensional features from "<>ToString@Length@features<>" images."];

log[1, "## Load ResNet-101 Trained on Augmented CASIA-WebFace Data"];
featureExtractor = NetModel["ResNet-101 Trained on Augmented CASIA-WebFace Data"];
log[2, "## Create a findNearest function returning index and distance"]
findNearestPhoto = FeatureNearest[features -> {"Index", "Distance"}];

log[1, "## All data loaded and initialized"];

findSimilarFaces[testImagePath_String, imageType_String, n_Integer] := Module[
  {image, faces, transformedFace, nearestFaces},

  byteArray = ReadByteArray[testImagePath];
  log[1, "## loaded "<>ToString@Length@byteArray<>" bytes from "<>testImagePath];

  Catch[image = ImportByteArray[byteArray]];
  log[1, "## Converted bytes to image and got an "<>ToString@Head@image];

  If[Not[Head@image === Image],
    log[0, "## ERROR: Unable to process image"];
    ExportString[<|"error"->"Unable to import image file."|>,"JSON"],

    log[1, "## ImageDimensions: "<>ToString@ImageDimensions[image]];
    log[1, "## Look for faces"];
    faces = FaceAlign[image, Automatic, {224,224}];
    log[1, "## Found "<>ToString@Length@faces<>" faces in image"];
    If[Length @ faces == 0,
      ExportString[<| "error" -> "Found no face" |>, "JSON"],

      log[2, "## Extract features from the B/W version of all the found faces, and find the nearest "<>ToString@n<>" faces of those."];
      nearestFaces = findNearestPhoto[featureExtractor@ColorConvert[#, "Grayscale"], n]&/@faces;

      log[1, "## image processed"];
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

findSimilarFaces[testImagePath_String, n_Integer] := Module[
  {image, faces, transformedFace, nearestFaces},

  byteArray = ReadByteArray[testImagePath];
  log[1, "## loaded "<>ToString@Length@byteArray<>" bytes from "<>testImagePath];

  Catch[image = ImportByteArray[byteArray]];
  log[1, "## Converted bytes to image and got an "<>ToString@Head@image];

  If[Not[Head@image === Image],
    log[0, "## ERROR: Unable to process image"];
    ExportString[<|"error"->"Unable to import image file."|>,"JSON"],

    log[1, "## ImageDimensions: "<>ToString@ImageDimensions[image]];
    log[1, "## Look for faces"];
    faces = FaceAlign[image, Automatic, {224,224}];
    log[1, "## Found "<>ToString@Length@faces<>" faces in image"];
    If[Length @ faces == 0,
      ExportString[<| "error" -> "Found no face" |>, "JSON"],

      log[2, "## Extract features from the B/W version of all the found faces, and find the nearest "<>ToString@n<>" faces of those."];
      nearestFaces = findNearestPhoto[featureExtractor@ColorConvert[#, "Grayscale"], n]&/@faces;

      log[1, "## image processed"];
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


log[1, "## DANER loaded and ready for requests."]
(* result = resultJSON[imageUrl,3];*)



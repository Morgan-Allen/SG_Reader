//
//  tests_class.hpp
//  SG_tools_cpp
//
//  Created by Morgan Allen on 12/11/2017.
//  Copyright © 2017 Morgan Allen. All rights reserved.
//

#ifndef tests_class_hpp
#define tests_class_hpp

#include <stdio.h>
#include <SDL.h>
#include "SG_Handler.hpp"
#include "SG_Utils_555.hpp"




void testFileIO(
    string basePath, string outputDir, string fileSG, int version, bool report
);

void testImagePacking(
    string basePath, string outputDir,
    string fileSG, string file555, int version,
    vector <string> testImageIDs, bool report
);


#endif

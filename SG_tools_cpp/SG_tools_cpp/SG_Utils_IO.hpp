//
//  SG_Utils_IO.hpp
//  SG_tools_cpp
//
//  Created by Morgan Allen on 12/11/2017.
//  Copyright © 2017 Morgan Allen. All rights reserved.
//

#ifndef SG_Utils_IO_hpp
#define SG_Utils_IO_hpp

#include "SG_Handler.hpp"
#include "SG_Utils_555.hpp"
#include <stdio.h>



void replaceSingleImage(
    string basePath, string fileSG, int version,
    string recordID, string newBMPpath, string outputDir
);

#endif

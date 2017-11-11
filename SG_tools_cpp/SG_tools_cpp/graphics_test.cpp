/*This source code copyrighted by Lazy Foo' Productions (2004-2015)
and may not be redistributed without written permission.*/

//Using SDL and standard IO
#include <SDL.h>
#include <stdio.h>
#include <iostream>
#include <string>



//  Snippets for later use...
 /*
 // Game related functions
 void GameApp::GameLoop(void)
 {
 RenderFrame();    
 }
 
 void GameApp::RenderFrame(void) 
 {
 glClear(GL_COLOR_BUFFER_BIT);
 glColor3f(0.7, 0.5, 0.8);
 glRectf(1.0, 1.0, 3.0, 2.0);
 SDL_GL_SwapBuffers();
 }
 //*/


int call_graphics() {
    
    // Create an application window with the following settings:
    SDL_Init(SDL_INIT_VIDEO);
    SDL_Window* window = SDL_CreateWindow(
        "An SDL2 window",                  // window title
        SDL_WINDOWPOS_UNDEFINED,           // initial x position
        SDL_WINDOWPOS_UNDEFINED,           // initial y position
        640,                               // width, in pixels
        480,                               // height, in pixels
        SDL_WINDOW_OPENGL                  // flags - see below
    );
    
    // Check that the window was successfully created
    if (window == NULL) {
        printf("Could not create window...");
        return 1;
    }
    
    std::string basePath = "/Users/morganallen/Desktop/Programming/SG Tools Project/";
    std::string imgPath  = basePath + "SG_tools_cpp/hello_world.bmp";
    
    SDL_Surface* image  = SDL_LoadBMP(imgPath.c_str());
    SDL_Surface* screen = SDL_GetWindowSurface(window);
    
    if (image == NULL || screen == NULL) {
        printf("Could not load image/initialise screen...");
        return 1;
    }
    
    // A basic main loop to prevent blocking
    bool is_running = true;
    SDL_Event event;
    while (is_running) {
        
        SDL_BlitSurface(image, NULL, screen, NULL);
        SDL_UpdateWindowSurface(window);
        
        while (SDL_PollEvent(&event)) {
            if (event.type == SDL_QUIT) {
                is_running = false;
            }
        }
        SDL_Delay(16);
    }
    
    //  Perform cleanup:
    SDL_FreeSurface(image);
    SDL_DestroyWindow(window);
    SDL_Quit();
    
    return 0;
}









# Fairy Tale Image Generation Server (Using MidJourney Proxy)
This project generates fairy tale character and background images using **MidJourney** and allows users to monitor the progress of image creation. Since MidJourney doesn't provide an official API, this server interacts with the MidJourney Proxy ([novicezk/midjourney-proxy](https://github.com/novicezk/midjourney-proxy)) to handle messages from the MidJourney Discord server.

<img width="1512" alt="image" src="https://github.com/user-attachments/assets/93108e72-7c72-4ff7-b7cd-dfb8efaf07a7">

### Character Prompt Generation: There are two methods for generating character prompts:
1. Selecting options for the protagonist's appearance (e.g., hairstyle, facial expression).
   <img width="780" alt="image" src="https://github.com/user-attachments/assets/3e03cd6a-cdef-4de0-b615-f73bd3968942">
2. Creating a character that closely resembles a hand-drawn sketch.
   <img width="780" alt="image" src="https://github.com/user-attachments/assets/f2233a92-b78a-4d8b-9c27-27973a193d48">



## Process Overview

### Features

- **Image Generation Request**: Accepts `studentTaskId`, fairy tale ID, and prompts for both characters and backgrounds from the client.
- **Main Character Image Creation**: Generates the main character image first to maintain consistent style across all images.
- **Asynchronous Image Generation**: Once the reference image is ready, other character and background images are processed in parallel, without waiting for all images to complete.
- **Image Progress Monitoring**: Clients can query the API periodically to check the `progress`, `imageUrl`, and the overall `completed` or `error` status.

### Image Generation Workflow

1. **Client Request**: The server receives `studentTaskId`, fairy tale ID, and prompts for characters and background.
2. **Main Character Image Creation**: The server sends an image creation request to the MidJourney Proxy for the main character, which serves as a reference image for the rest.
3. **Asynchronous Image Creation**: Once the reference image is ready, other images (characters and background) are processed concurrently in the background.
4. **Post-processing**: After image generation, images may go through post-processing such as background removal or blurring.

### Detailed Image Generation Steps

1. **IMAGINE Request**:
   - The server sends an `IMAGINE` request to the MidJourney Proxy.
   - The server fetches the status periodically and updates Firebase with the success/failure status until the process is complete.
   
2. **UPSCALE Request**:
   - After the `IMAGINE` request generates four images, the server requests to upscale a specific index.
   - The `UPSCALE` request is also handled by periodically fetching its status, and the final image is saved.


## Main API Endpoints

### 1. Image Generation Request
Handles the initial request to generate character and background images using the provided prompts.

### 2. Image Status Query
Returns the current progress, image URL, and completion/error status for the images being generated.

### 3. Regenerate Image API
Allows the client to regenerate a specific image by providing the imageId. This API triggers the creation of only the selected image without affecting the others.


## [MidJourney Proxy](https://github.com/novicezk/midjourney-proxy) Config Updates

- **Timeout Configuration**: Resolved issues where some image generation progress wasn't being properly tracked. Set IMAGINE timeout to 2 minutes and UPSCALE timeout to 30 seconds.
- **Concurrency Adjustments**: Adjusted `coreSize` and `queueSize` based on the MidJourney Pro Plan, allowing for 12 concurrent tasks.

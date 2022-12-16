# RDE client app (android)

This repository contains the source code for the RDE client app for Android.
It uses the [RDE Java client library]() for implementing RDE functionality, which is included as a submodule.

## Usage
The app implements the following functionality:
- Enrolling an e-passport for RDE and pushing the enrollment parameters to the server
- Retrieving secret keys from an e-passport communicating via websockets with the RDE JS client

### Enrolling
To enroll an e-passport for RDE, the app needs to scan a QR code containing the HTTP endpoint to which the enrollment parameters should be pushed.
The app then offers a screen to enter some information about the document that is required for communicating with the document (the BAC key).
Afterwards, the app will give the user to set certain options for the enrollment process.
After the enrollment process is finished, the enrollment parameters are pushed to the url specified in the QR code.

### Retrieving secret keys
To retrieve secret keys from an e-passport, the app needs to scan a QR code containing a WebSocket URL.
The app will follow the DecryptionHandshake protocol to set up a tunnel to the RDE JS client via the WebSocket.
It then receives the DecryptionParameters object from the RDE JS client and uses it to retrieve the secret key from the e-passport, which is then sent back to the RDE JS client.

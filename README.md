# ParkPulse

The ParkPulse application is a community-driven solution enabling neighbors to effortlessly notify each other about their upcoming car movements. Users can subscribe to receive notifications when cars are set to be relocated within their vicinity.

## Key Features

   > **User Management**: Seamless registration and user profiles allowing for convenient adjustments to personal details such as passwords and addresses.

   > **Task Execution**: Data management integrated with cloud storage (Firebase) ensures the secure handling and storage of information.
   

## Functionalities

   **Cloud Data Storage**: All data is securely stored in the cloud, utilizing Firebase's robust infrastructure.
   **Real-Time Mapping**: Visual representation on a map displaying the exact locations of cars preparing to move, accompanied by scheduled timings

The ParkPulse application serves as a practical tool for local communities, fostering convenience and coordination among neighbors regarding car movements. By harnessing technology to connect users and streamline communication, this app simplifies the process of managing parking logistics within neighborhoods.

## Json of the database

```json
{
  "parking": {
    "25590324": {
      "carId": "25590324",
      "lat": 52.3795112,
      "location": "27, Potgieterstraat, Haarlem, Noord-Holland, Nederland, 2032 VM, Nederland",
      "lon": 4.6512843,
      "person": {
        "name": "John Doe",
        "personId": "John Doe"
      },
      "time": 4
    },
    "31605114": {
      "carId": "31605114",
      "lat": 52.3273491,
      "location": "11, Ellermanstraat, Amstel Business Park, Amsterdam-Duivendrecht, Ouder-Amstel, Noord-Holland, Nederland, 1114 AK, Nederland",
      "lon": 4.9299273,
      "person": {
        "name": "Sarah Doe",
        "personId": "Sarah Doe"
      },
      "time": 3
    },
    "214661133": {
      "carId": "214661133",
      "lat": 51.228867449999996,
      "location": "17, Duboisstraat, Eilandje, Antwerpen, Vlaanderen, België / Belgique / Belgien",
      "lon": 4.414982607905316,
      "person": {
        "name": "Philip Doe",
        "personId": "Philip Doe"
      },
      "time": 3
    }
  },
  "person": {
    "John": {
      "name": "John Doe",
      "personId": "John Doe"
    },
    "Sarah": {
      "name": "Sarah Doe",
      "personId": "Sarah Doe"
    },
    "Phili^p": {
      "name": "Philip Doe",
      "personId": "Philip Doe"
    }
  }
}
```


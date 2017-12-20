import Foundation
import CoreLocation

class CoreLocationController : NSObject, CLLocationManagerDelegate {
    
    var locationManager:CLLocationManager = CLLocationManager()
    var viewController:ViewController!
    
    override init() {
        super.init()
        self.locationManager.delegate = self
        //self.locationManager.requestAlwaysAuthorization()
        self.locationManager.requestWhenInUseAuthorization()
        if #available(iOS 9.0, *) {
            self.locationManager.allowsBackgroundLocationUpdates = true
        } else {
            // Fallback on earlier versions
        }
    }
    
    func locationManager(manager: CLLocationManager, didChangeAuthorizationStatus status:CLAuthorizationStatus) {
        print("didChangeAuthorizationStatus", terminator: "")
        
        switch status {
        case .NotDetermined:
            print(".NotDetermined", terminator: "")
            break
            
        case .AuthorizedWhenInUse, .AuthorizedAlways:
            print(".Authorized", terminator: "")
            self.startLocationTracking()
            break
            
        case .Denied:
            print(".Denied", terminator: "")
            break
            
        default:
            print("Unhandled authorization status", terminator: "")
            break
        }
    }
    
    func startLocationTracking() {
        self.locationManager.startUpdatingLocation()
    }
    
    func stopLocationTracking() {
        self.locationManager.stopUpdatingLocation()
    }
    
    func locationManager(manager: CLLocationManager, didUpdateLocations locations: [CLLocation]) {
        
        let location = locations.last!
        
        print("didUpdateLocations:  \(location.coordinate.latitude), \(location.coordinate.longitude)", terminator: "")
        viewController.setLocation(location)
    }
}
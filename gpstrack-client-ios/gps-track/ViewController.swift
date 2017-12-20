import CoreLocation
import MapKit
import UIKit

class ViewController: UIViewController, UITableViewDataSource, UITableViewDelegate, MKMapViewDelegate, NSURLSessionDelegate {
    
    @IBOutlet weak var latitudeLabel: UILabel!
    @IBOutlet weak var longitudeLabel: UILabel!
    @IBOutlet weak var towersTableView: UITableView!
    @IBOutlet weak var mapView: MKMapView!
    @IBOutlet weak var distanceSlider: UISlider!
    @IBOutlet weak var startButton: UIBarButtonItem!
    @IBOutlet weak var stopButton: UIBarButtonItem!
    @IBOutlet weak var unlockMapButton: UIBarButtonItem!
    
    var towersData: NSArray!
    var foundInitialLocation: Bool = false
    var mapLocked: Bool = true
    
    override func viewDidLoad() {
        super.viewDidLoad()
        // Do any additional setup after loading the view, typically from a nib.
        let appDelegate = UIApplication.sharedApplication().delegate as! AppDelegate
        appDelegate.coreLocationController?.viewController = self
        towersTableView.dataSource = self
        towersTableView.delegate = self
    }
    
    override func didReceiveMemoryWarning() {
        super.didReceiveMemoryWarning()
        // Dispose of any resources that can be recreated.
    }
    
    /*
    @IBAction func buttonPressed(sender: UIButton) {
        print("buttonPressed", terminator: "")
        let long = longitudeLabel.text ?? ""
        let lat = latitudeLabel.text ?? ""
        calcNearby(long, latitude: lat)
    }
*/
    
    @IBAction func startLocationTracking(sender: UIBarButtonItem) {
        print("startLocationTracking", terminator: "")
        let appDelegate = UIApplication.sharedApplication().delegate as! AppDelegate
        appDelegate.coreLocationController?.startLocationTracking()
        startButton.tintColor = UIColor.lightGrayColor()
        stopButton.tintColor = UIColor.blueColor()
    }

    @IBAction func stopLocationTracking(sender: UIBarButtonItem) {
        print("stopLocationTracking", terminator: "")
        let appDelegate = UIApplication.sharedApplication().delegate as! AppDelegate
        appDelegate.coreLocationController?.stopLocationTracking()
        stopButton.tintColor = UIColor.lightGrayColor()
        startButton.tintColor = UIColor.blueColor()
    }
    
    @IBAction func toggleMapLock(sender: AnyObject) {
        mapLocked = !mapLocked
        unlockMapButton.title = mapLocked ? "Unlock Map" : "Lock Map"
    }
    
    func calcNearby(longitude: String, latitude: String) {
        self.post(["latitude":latitude, "longitude":longitude], url: "https://nileshk.com:14443/nearby", logResponse: true) { (succeeded: Bool, msg: String) -> () in
            print("nearby sent", terminator: "")
        }
    }
    
    func sendLocation(longitude: Double, latitude: Double, distance: Double) {
        print("sending location", terminator: "")
        self.post(["latitude":latitude.description, "longitude":longitude.description, "distance": distance.description], url: "https://nileshk.com:14443/currentLocation", logResponse: false) { (succeeded: Bool, msg: String) -> () in
            print("sent location", terminator: "")
        }
        
    }
    
    func setLocation(location: CLLocation) {
        print("setLocation called", terminator: "")
        latitudeLabel.text = location.coordinate.latitude.description
        longitudeLabel.text = location.coordinate.longitude.description

        print("Distance: \(distanceSlider.value)", terminator: "")
        
        sendLocation(location.coordinate.longitude as Double, latitude: location.coordinate.latitude as Double, distance: Double(distanceSlider.value))
        calcNearby(location.coordinate.longitude.description, latitude: location.coordinate.latitude.description)
        
        if !foundInitialLocation || mapLocked {
            let span = MKCoordinateSpanMake(0.005, 0.005)
            let region: MKCoordinateRegion = MKCoordinateRegion(center: location.coordinate, span: span)
            mapView.setRegion(region, animated: true)
        }
        foundInitialLocation = true
    }
    
    func post(params : Dictionary<String, String>, url : String, logResponse: Bool, postCompleted : (succeeded: Bool, msg: String) -> ()) {
        //print("post")
        
        // set up the base64-encoded credentials
        let username = "user"
        let password = "password_changeme"
        let loginString = NSString(format: "%@:%@", username, password)
        let loginData: NSData = loginString.dataUsingEncoding(NSUTF8StringEncoding)!
        let base64LoginString = loginData.base64EncodedStringWithOptions([])
        
        let request = NSMutableURLRequest(URL: NSURL(string: url)!)
        request.HTTPMethod = "POST"
        request.setValue("Basic \(base64LoginString)", forHTTPHeaderField: "Authorization")
    
        let session = NSURLSession(configuration: NSURLSessionConfiguration.defaultSessionConfiguration(), delegate: self, delegateQueue: NSOperationQueue.mainQueue())
        let writingOptions = NSJSONWritingOptions()
        //print("Serializing JSON")
        do {
            try request.HTTPBody = NSJSONSerialization.dataWithJSONObject(params, options: writingOptions)
        } catch {
            print("Error")
            return
        }
        request.addValue("application/json", forHTTPHeaderField: "Content-Type")
        request.addValue("application/json", forHTTPHeaderField: "Accept")
        
        let task = session.dataTaskWithRequest(request, completionHandler: {data, response, error -> Void in
            if (logResponse) {
                print("\nResponse: \(response)", terminator: "")
                if let data = data {
                    if (data.length > 0) {
                        let strData = NSString(data: data, encoding: NSUTF8StringEncoding)
                        print("Body: \(strData)", terminator: "")
                        let error:NSError? = nil
                        let json = (try! NSJSONSerialization.JSONObjectWithData(data, options: .AllowFragments)) as! NSArray
                        if (error != nil) {
                            print(error, terminator: "")
                            return
                        }
                        self.towersData = json
                        for tower in json {
                            let longLat: NSDictionary = tower["longLat"] as! NSDictionary
                            let lat = longLat["latitude"] as! Double
                            let long = longLat["longitude"] as! Double
                            let poi = PointOfInterest(title:  tower["name"] as! String, coordinate: CLLocationCoordinate2D(latitude: lat, longitude: long), info: tower["name"] as! String)
                            self.mapView.addAnnotation(poi)
                        }
                        dispatch_async(dispatch_get_main_queue(), { () -> Void in
                            self.towersTableView.reloadData()
                        })
                        print("json: \(json)", terminator: "")
                    }
                }
            }
        })
        
        print("Task resume", terminator: "")
        task.resume()
    }
    
    // UITableViewDataSource Functions
    
    func tableView(tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        if let towers = self.towersData {
            print("Towers count: \(towers.count)", terminator: "")
            return towers.count
        } else {
            return 0
        }
    }
    
    func tableView(tableView: UITableView, cellForRowAtIndexPath indexPath: NSIndexPath) ->   UITableViewCell {
        let cell = UITableViewCell()
        let label = UILabel(frame: CGRect(x:0, y:0, width:400, height:50))
        if let towers = self.towersData {
            let tower = towers[indexPath.row] as? NSDictionary
            if let tower = tower {
                label.text = tower["name"] as? String
            }
        } else {
            label.text = "undefined"
        }
        cell.addSubview(label)
        return cell
    }
    
    
    // UITableViewDelegate Functions
    
    func tableView(tableView: UITableView, heightForRowAtIndexPath indexPath: NSIndexPath) -> CGFloat {
        return 50
    }

    // MKMapKitViewDelegate
    
    func mapView(mapView: MKMapView, viewForAnnotation annotation: MKAnnotation) -> MKAnnotationView? {
        let identifier = "PointOfInterest"
        
        if annotation.isKindOfClass(PointOfInterest.self) {
        var annotationView = mapView.dequeueReusableAnnotationViewWithIdentifier(identifier)
        
        if annotationView == nil {
        annotationView = MKPinAnnotationView(annotation:annotation, reuseIdentifier:identifier)
        annotationView?.enabled = true
        annotationView?.canShowCallout = true
        } else {
        annotationView?.annotation = annotation
        }
        
        return annotationView
        }
        return nil
    }
    
    func mapView(mapView: MKMapView, annotationView view: MKAnnotationView, calloutAccessoryControlTapped control: UIControl) {
        let poi = view.annotation as! PointOfInterest
        let placeName = poi.title
        let placeInfo = poi.info
        
        let ac = UIAlertController(title: placeName, message: placeInfo, preferredStyle: .Alert)
        ac.addAction(UIAlertAction(title: "OK", style: .Default, handler: nil))
        presentViewController(ac, animated: true, completion: nil)
    }
    
    func URLSession(session: NSURLSession, didReceiveChallenge challenge: NSURLAuthenticationChallenge, completionHandler: ((NSURLSessionAuthChallengeDisposition, NSURLCredential?) -> Void)) {
        completionHandler(NSURLSessionAuthChallengeDisposition.UseCredential, NSURLCredential(forTrust: challenge.protectionSpace.serverTrust!))
    }
    
}

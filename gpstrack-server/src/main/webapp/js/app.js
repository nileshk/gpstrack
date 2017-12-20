$(document).ready(function () {
	var latitude = -82.53;
	var longitude = 27.97;
	var currentTime = Date.now();
	var tenMinutesMillis = 1000 * 60 * 10;
	var hourMillis = 1000 * 60 * 60;
	var dayMillis = 12 * hourMillis;
	var weekMillis = 7 * dayMillis;
	var monthMillis = 4 * weekMillis;

	var stamenTonerSource = new ol.source.Stamen({
		layer: 'toner'
	});
	var osmSource = new ol.source.OSM();

	var map = new ol.Map({
		target: 'map',
		layers: [
			new ol.layer.Tile({
				source: stamenTonerSource
			})
		],
		view: new ol.View({
			center: ol.proj.transform([latitude, longitude], 'EPSG:4326', 'EPSG:3857'),
			zoom: 12
			// maxZoom: 17
		})
	});
	var view = map.getView();

	var geoJSONFormat = new ol.format.GeoJSON({
		'defaultDataProjection': 'EPSG:4326' // view.getProjection()
	});

	var source = new ol.source.GeoJSON({
		projection: view.getProjection()
	});

	var layer = new ol.layer.Vector({
		title: 'Points of Interest',
		source: source
	});

	map.addLayer(layer);

	console.log("Attempting to fetch points of interest data...");

	$.ajax({
		url: pointsOfInterestUrl,
		type: 'GET',
		dataType: 'json',
		contentType: 'application/json',
		mimeType: 'application/json',
		success: function (data) {
			console.log("Successfully fetched points of interest data, processing...");
			currentTime = Date.now();
			var features = geoJSONFormat.readFeatures(data, {featureProjection: view.getProjection()});
			console.log('Fetched ' + features.length + ' points of interest');
			//console.log(features);
			source.addFeatures(features);
			console.log("Done");
		},
		error: function (data, status, er) {
			console.log(er);
		}
	});

	$.ajax({
		url: locationHistoryUrl,
		type: 'GET',
		dataType: 'json',
		contentType: 'application/json',
		mimeType: 'application/json',
		success: function (data) {
			console.log("Successfully fetched location history data, processing...");
			currentTime = Date.now();
			var features = geoJSONFormat.readFeatures(data, {featureProjection: view.getProjection()});
			console.log('Fetched ' + features.length + ' location history items');
			//console.log(features);
			source.addFeatures(features);
			console.log("Done");
		},
		error: function (data, status, er) {
			console.log(er);
		}
	});

	var element = document.getElementById('popup');

	var popup = new ol.Overlay({
		element: element,
		positioning: 'bottom-center',
		stopEvent: false
	});
	map.addOverlay(popup);

	// display popup on click
	map.on('click', function (evt) {
		var feature = map.forEachFeatureAtPixel(evt.pixel,
			function (feature, layer) {
				console.log(feature);
				return feature;
			});
		if (feature) {
			var geometry = feature.getGeometry();
			var coord = geometry.getCoordinates();
			popup.setPosition(coord);
			$(element).popover('destroy')
				.popover({
					'placement': 'top',
					'html': true,
					'content': getContent(feature)
				})
				.popover('show');
		} else {
			$(element).popover('destroy');
		}
	});

	function getContent(feature) {
		var values = feature.values_;
		return '<b>' + feature.get('name') + '</b><br/>' +
			'Last updated: ' + values.submit_timestamp + '<br/>' +
			'Guid: ' + values.guid + '<br/>';
	}

});

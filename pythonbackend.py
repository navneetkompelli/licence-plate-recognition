import cv2
import imutils
import numpy as np
import datetime as dt
import pytesseract
pytesseract.pytesseract.tesseract_cmd = r'/usr/bin/tesseract'
import firebase_admin
from firebase_admin import credentials
from firebase_admin import db
import requests
import json
serverKey = 'AAAApwpu7pU:APA91bHAz8ijoBTkMvm75lQ_vwA52X6lgJdR_RaVlGWwQkaczmVmgywKmwFcSFdAxUKtRps5vsOICcQHorg5oLGMt1NFLVmJWlgBp2IB-Ma44mhoNNsyhcASBd_3PcjokftnXC7TIMef'
cred = credentials.Certificate('privkeyfbproj01.json')
firebase_admin.initialize_app(cred, {
    'databaseURL': 'https://fbproj01.firebaseio.com'
})

camera = cv2.VideoCapture(2)
while True:
    read, frame = camera.read()
    cv2.imshow("captureShow", frame)
    key = cv2.waitKey(1)
    if key%256 == 27: # ESC key hit
        print("ESC, closed")
        break
    elif key%256 == 32: # SPACE key hit
        temptime = dt.datetime.now().strftime('%Y_%m_%d_%H_%M_%S_%f')[:-3] # timestamp including milliseconds
        img_name = temptime+".png"
        cv2.imwrite(img_name, frame) # save image 
        print("{} written!".format(img_name))
        break
camera.release()
cv2.destroyAllWindows()

image = cv2.imread(img_name, cv2.IMREAD_COLOR) # read the saved image
image = cv2.resize(image, (600,400)) # resize all images to 600x400 to avoid bias 
grey = cv2.cvtColor(image, cv2.COLOR_BGR2GRAY) # convert image to grayscale
grey = cv2.bilateralFilter(grey, 13, 15, 15)  # Reduce the noise in the image by blurring
edgedimage = cv2.Canny(grey, 30, 200) # detect edges in the image
findcontours = cv2.findContours(edgedimage.copy(), cv2.RETR_TREE, cv2.CHAIN_APPROX_SIMPLE) # find contours
grabcontours = imutils.grab_contours(findcontours) # grab contours
contours10 = sorted(grabcontours, key = cv2.contourArea, reverse = True)[:10] # sort and store only the largest ten contours
count = None

for c in contours10: # find contours that are of rectangle shape
    arclen = cv2.arcLength(c, True)
    approx = cv2.approxPolyDP(c, 0.018 * arclen, True)
    if len(approx) == 4:
        count = approx
        break

if count is None:
    print ("No rectangle contour detected")
else:
    cv2.drawContours(image, [count], -1, (0, 0, 255), 3)
    mask = np.zeros(grey.shape, np.uint8) # mask the part of the image that is not the plate
    drawcontours = cv2.drawContours(mask, [count], 0,255,-1,)
    (a, b) = np.where(mask == 255)
    (minx, miny) = (np.min(a), np.min(b)) # crop the plate part of the image
    (maxx, maxy) = (np.max(a), np.max(b))
    croppedimage = grey[minx:maxx+1, miny:maxy+1]

    text = pytesseract.image_to_string(croppedimage, config='--psm 11') # Read the characters from the cropped image
    cleaned = "".join([char if ord(char) < 128 else "" for char in text]).strip() # clean the tesseract output to remove non-ascii characters
    print (cleaned)

    ref = db.reference('/tokensplates') # connect to firebase db
    snapshot = db.reference('/tokensplates') # data is stored in this node with platenumber as key and devicetoken as value
    snapshot = ref.order_by_key().get() # fetch the data from db, order by platenumbers

    for key, val in snapshot.items():
        if key == cleaned: # Match found in database
            deviceToken = val; # store the corresponding device token. Notification will be sent only to this token (device).
            print("Match Found, Sending notification to the Parking App")
            headers = {
                    'Content-Type': 'application/json',
                    'Authorization': 'key=' + serverKey,
                  }
            body = {
                      'notification': {'title': 'Parking Access Granted',
                                        'body': 'Your vehicle is granted access at ' + temptime
                                        },
                      'to':
                          deviceToken,
                      'priority': 'high',
                    }
            response = requests.post("https://fcm.googleapis.com/fcm/send",headers = headers, data=json.dumps(body))
            if response.status_code == 200:
                print("Notification Sent")
            else:
                print("Error while sending notification")
            break
        else: # No match found in database
            print("No Match Found")


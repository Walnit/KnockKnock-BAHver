from flask import Flask, abort, jsonify

app = Flask(__name__)

messageStore = {}
keyBundleStore = {}

@app.route("/knock/sendBundle/<name>/<bundle>")
def send_bundle(name, bundle):
        keyBundleStore[name] = bundle
        return jsonify(success=True)

@app.route("/knock/getBundle/<name>")
def get_bundle(name):
        if name in keyBundleStore: return keyBundleStore[name]
        else: return abort(404)


@app.route("/knock/sendMessages/<receiver>/<sender>/<payload>")
def send_message(receiver, sender, payload):
        if receiver in messageStore:
                if sender in messageStore[receiver]:
                        messageStore[receiver][sender].append(payload)
                else:
                        messageStore[receiver][sender] = [payload]
        else:
                messageStore[receiver] = {sender: [payload]}
        return jsonify(success=True)

@app.route("/knock/receiveMessages/<receiver>/<sender>")
def recieveMesages(receiver, sender):
        if receiver in messageStore and sender in messageStore[receiver]:
                return messageStore[receiver][sender]
        return abort(404)
if __name__ == '__main__':
        app.debug = True
        app.run(host="0.0.0.0", port=8907)
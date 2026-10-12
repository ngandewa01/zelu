import React from 'react';
import { AppRegistry, ScrollView, Text, View } from 'react-native';
import App from './App';
import { name as appName } from './app.json';

// Capture any error that happens outside React rendering (startup, async,
// native bridge) and show it on screen instead of crashing silently.
let globalError = null;
const listeners = new Set();
function reportError(err) {
  globalError = err;
  listeners.forEach(fn => fn());
}

const defaultHandler =
  global.ErrorUtils && global.ErrorUtils.getGlobalHandler
    ? global.ErrorUtils.getGlobalHandler()
    : null;
if (global.ErrorUtils && global.ErrorUtils.setGlobalHandler) {
  global.ErrorUtils.setGlobalHandler((err, isFatal) => {
    reportError(err);
    if (defaultHandler) {
      try {
        defaultHandler(err, false);
      } catch (_) {}
    }
  });
}

function ErrorScreen({ error }) {
  const msg = error && error.message ? error.message : String(error);
  const stack = error && error.stack ? error.stack : '';
  return (
    <ScrollView style={{ flex: 1, backgroundColor: '#fff' }} contentContainerStyle={{ padding: 20, paddingTop: 60 }}>
      <Text style={{ fontSize: 18, fontWeight: '700', color: '#b00', marginBottom: 10 }}>
        Zelu hit an error
      </Text>
      <Text selectable style={{ fontSize: 14, color: '#000', marginBottom: 14 }}>
        {msg}
      </Text>
      <Text selectable style={{ fontSize: 11, color: '#444', fontFamily: 'monospace' }}>
        {stack}
      </Text>
    </ScrollView>
  );
}

class Root extends React.Component {
  constructor(props) {
    super(props);
    this.state = { error: globalError };
    this._onGlobal = () => this.setState({ error: globalError });
  }
  componentDidMount() {
    listeners.add(this._onGlobal);
    if (globalError) {
      this.setState({ error: globalError });
    }
  }
  componentWillUnmount() {
    listeners.delete(this._onGlobal);
  }
  componentDidCatch(error) {
    this.setState({ error });
  }
  static getDerivedStateFromError(error) {
    return { error };
  }
  render() {
    if (this.state.error) {
      return <ErrorScreen error={this.state.error} />;
    }
    try {
      return <App />;
    } catch (e) {
      return <ErrorScreen error={e} />;
    }
  }
}

AppRegistry.registerComponent(appName, () => Root);

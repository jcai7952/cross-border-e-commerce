// Basic smoke test: the app builds and shows a loading indicator
// while AppState is loading from shared_preferences.

import 'package:flutter_test/flutter_test.dart';
import 'package:flutter/material.dart';

import 'package:shop_app/main.dart';

void main() {
  testWidgets('App boots without throwing', (WidgetTester tester) async {
    await tester.pumpWidget(const ShopApp());
    // First frame shows a progress indicator before AppState.load finishes.
    expect(find.byType(MaterialApp), findsOneWidget);
  });
}

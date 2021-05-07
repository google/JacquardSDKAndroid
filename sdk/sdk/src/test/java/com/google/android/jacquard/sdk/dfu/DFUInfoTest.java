/*
 * Copyright 2021 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.android.jacquard.sdk.dfu;

import static com.google.common.truth.Truth.assertThat;

import android.os.Build.VERSION_CODES;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.google.android.jacquard.sdk.dfu.DFUInfo;
import com.google.android.jacquard.sdk.model.Revision;
import java.net.URI;
import java.net.URISyntaxException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;

@RunWith(AndroidJUnit4.class)
@Config(sdk = {VERSION_CODES.P}, manifest = Config.NONE)
public class DFUInfoTest {

  private final String version = "000006000";
  private final String dfuStatus = "optional";
  private final URI downloadUrl = new URI("https://storage.googleapis.com/wardrobeapi-jacquard-devint.appspot.com/firmware/42-f9-a1-e3/73-d0-58-c3/a9-46-5b-d3/000007000.bin?GoogleAccessId=wardrobeapi-jacquard-devint%40appspot.gserviceaccount.com&Expires=1623305434&Signature=Br0GNmhynsFXtBFwW6B1fD07gW4IPKw0g4GC4z6Isb5BmXSfcmUTfqPHKMipO5609h5w2CBxtenYB6UQ2iM7R7XM%2BBfoIYdOwSmzV8%2BeXjKfvC1U%2B74ET8vtNQSndUStoL8O31MDUmBW5jIlvTROtVak6jzvCDQ2uN2viCY7Bwu4mNkyBJQORvzXIz7RAtz9PFZnA0uvwzKuoSiMBDB3kCoJrCmtzNqt9dH6cbo9IQftFxqZlLWFaWJspgX7auvPW8ZmhMh7i8UPb%2BpYqhGNjBeTzzSOv9SXGYd926FVx1u8cabOUczs1W6m%2F5TqlOzHZc0x%2Fu4cffH7hVBYuXAekw%3D%3D");
  private final String vendorId = "42-f9-a1-e3";
  private final String productId = "73-d0-58-c3";
  private final String moduleId = "a9-46-5b-d3";

  public DFUInfoTest() throws URISyntaxException {
  }

  @Test
  public void givenValueTypeWithAutoValue_whenFieldsCorrectlySet_thenCorrect() {
    // Act
    DFUInfo dFUInfo = DFUInfo.create(version,dfuStatus, downloadUrl,vendorId,productId,moduleId);

    // Assert
    assertThat(dFUInfo.version()).isEqualTo(Revision.fromZeroString(version));
    assertThat(dFUInfo.dfuStatus()).isEqualTo(DFUInfo.UpgradeStatus.OPTIONAL);
    assertThat(dFUInfo.downloadUrl()).isEqualTo(downloadUrl);
    assertThat(dFUInfo.vendorId()).isEqualTo(vendorId);
    assertThat(dFUInfo.productId()).isEqualTo(productId);
    assertThat(dFUInfo.moduleId()).isEqualTo(moduleId);
  }

  @Test
  public void givenTwoEqualValueTypeWithAutoValue_whenEqual_thenCorrect() {

    // Act
    DFUInfo dFUInfo1 = DFUInfo
        .create(version, dfuStatus, downloadUrl, vendorId, productId, moduleId);
    DFUInfo dFUInfo2 = DFUInfo
        .create(version, dfuStatus, downloadUrl, vendorId, productId, moduleId);

    // Assert
    assertThat(dFUInfo1.equals(dFUInfo2)).isTrue();
  }

  @Test
  public void givenTwoEqualValueTypeWithAutoValue_whenNotEqual_thenCorrect() {

    // Act
    DFUInfo dFUInfo1 = DFUInfo
        .create(version, dfuStatus, downloadUrl, vendorId, productId, moduleId);
    DFUInfo dFUInfo2 = DFUInfo
        .create(version, dfuStatus, downloadUrl, vendorId, productId, "NewModuleId");

    // Assert
    assertThat(dFUInfo1.equals(dFUInfo2)).isFalse();
  }

  @Test(expected = NullPointerException.class)
  public void givenValueTypeWithAutoValue_whenAnyValueNullExceptModuleId_thenCorrect() {

    // Assert
    DFUInfo dFUInfo = DFUInfo
        .create(version, dfuStatus, downloadUrl, vendorId, null, moduleId);
  }

  @Test
  public void givenValueTypeWithAutoValue_whenModuleIdNull_thenCorrect() {

    // Assert
    DFUInfo dFUInfo = DFUInfo
        .create(version, dfuStatus, downloadUrl, vendorId, productId, null);
    assertThat(dFUInfo.moduleId()).isNull();
  }
}

/*
 * Copyright (C) 2022 The Android Open Source Project
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
 * limitations under the License
 */

package com.android.settings.applications.appinfo;

import android.content.Context;
import android.content.pm.SigningInfo;
import android.content.pm.Signature;

import java.io.InputStream;
import java.io.ByteArrayInputStream;
import java.security.cert.X509Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.CertificateException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class AppSignaturesPreferenceController extends AppInfoPreferenceControllerBase {

    public AppSignaturesPreferenceController(Context context, String key) {
        super(context, key);
    }

    private String getCertificateDescription(byte[] certificateBytes) {
        String sha256Hash = "";
        String dn = "";

        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(certificateBytes);
            for(byte b : hash) {
                sha256Hash += String.format("%02X:", b);
            }
            sha256Hash = sha256Hash.substring(0, sha256Hash.length() - 1); // remove last colon
        } catch(NoSuchAlgorithmException e) {
            sha256Hash = "(error calculating fingerprint)";
        }

        try {
            CertificateFactory factory = CertificateFactory.getInstance("X509");
            X509Certificate cert = (X509Certificate) factory.generateCertificate(new ByteArrayInputStream(certificateBytes));
            dn = cert.getSubjectDN().toString();
        } catch(CertificateException e) {
            dn = "(error parsing certificate)";
        }

        return "SHA-256=" + sha256Hash + ", " + dn;
    }

    @Override
    public CharSequence getSummary() {
        SigningInfo signingInfo = mParent.getPackageInfo().signingInfo;
        if(signingInfo == null) {
            return "no signing information (?)";
        }

        String summary = signingInfo.hasMultipleSigners() 
            ? "APK signed by the following certificates:\n"
            : "APK signing certificate history:\n";

        Signature[] signers = signingInfo.hasMultipleSigners()
            ? signingInfo.getApkContentsSigners()
            : signingInfo.getSigningCertificateHistory();

        for(Signature signer : signers) {
            summary += "- " + getCertificateDescription(signer.toByteArray()) + "\n";
        }

        return summary;
    }
}
